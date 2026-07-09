// BhojanBandhu Shared JavaScript Utilities
const apiBaseUrl = window.location.origin.includes(':8081') || window.location.origin.includes('localhost') 
    ? "http://localhost:8081" 
    : "http://localhost:8081"; // Fallback to local Spring Boot port

// Fetch wrapper for clean API requests
async function apiFetch(endpoint, options = {}) {
    const defaultHeaders = {
        'Content-Type': 'application/json'
    };

    const token = localStorage.getItem('bb_token');
    if (token) {
        defaultHeaders['Authorization'] = `Bearer ${token}`;
    }

    const config = {
        ...options,
        headers: {
            ...defaultHeaders,
            ...options.headers
        }
    };

    if (config.body && typeof config.body === 'object') {
        config.body = JSON.stringify(config.body);
    }

    const response = await fetch(`${apiBaseUrl}${endpoint}`, config);

    if (!response.ok) {
        let errorMessage = 'Request failed';
        try {
            const errJson = await response.json();
            errorMessage = errJson.message || errJson.error || errorMessage;
        } catch (e) {
            errorMessage = response.statusText || errorMessage;
        }
        throw new Error(errorMessage);
    }

    // Some endpoints return 204 or empty bodies
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
        return await response.json();
    }
    return null;
}

// Auth State Management
function getCurrentUser() {
    const userJson = localStorage.getItem('bb_user');
    try {
        return userJson ? JSON.parse(userJson) : null;
    } catch (e) {
        return null;
    }
}

function checkAuth(requiredRole) {
    const user = getCurrentUser();
    if (!user) {
        window.location.href = 'index.html';
        return null;
    }
    if (requiredRole && user.role !== requiredRole) {
        // Misaligned role, send to respective dashboard or index
        if (user.role === 'PROVIDER') {
            window.location.href = 'provider.html';
        } else if (user.role === 'CUSTOMER') {
            window.location.href = 'customer.html';
        } else {
            window.location.href = 'index.html';
        }
        return null;
    }
    return user;
}

function logout() {
    localStorage.removeItem('bb_user');
    localStorage.removeItem('bb_token');
    window.location.href = 'index.html';
}

// Local Storage Mock Tracking Helpers (to supplement missing GET endpoints for orders/subscriptions)
const StorageSync = {
    // Subscriptions
    saveSubscription(sub, planName, providerOrgName) {
        const subs = this.getSubscriptions();
        const record = {
            ...sub,
            planName: planName || `Plan #${sub.planId}`,
            providerOrgName: providerOrgName || `Mess #${sub.providerId}`
        };
        subs.push(record);
        localStorage.setItem('bb_subscriptions', JSON.stringify(subs));
    },

    getSubscriptions(customerId = null) {
        const subsJson = localStorage.getItem('bb_subscriptions');
        let subs = [];
        try {
            subs = subsJson ? JSON.parse(subsJson) : [];
        } catch (e) {
            subs = [];
        }
        if (customerId) {
            return subs.filter(s => s.customerId == customerId);
        }
        return subs;
    },

    // Orders
    saveOrder(order, itemsText, providerOrgName) {
        const orders = this.getOrders();
        const record = {
            ...order,
            itemsText: itemsText || 'Meal items',
            providerOrgName: providerOrgName || `Mess #${order.providerId}`
        };
        orders.push(record);
        localStorage.setItem('bb_orders', JSON.stringify(orders));
    },

    getOrders(role = null, id = null) {
        const ordersJson = localStorage.getItem('bb_orders');
        let orders = [];
        try {
            orders = ordersJson ? JSON.parse(ordersJson) : [];
        } catch (e) {
            orders = [];
        }
        if (role === 'CUSTOMER' && id) {
            return orders.filter(o => o.customerId == id);
        }
        if (role === 'PROVIDER' && id) {
            return orders.filter(o => o.providerId == id);
        }
        return orders;
    },

    updateOrderStatus(orderId, status) {
        const orders = this.getOrders();
        const idx = orders.findIndex(o => o.id == orderId);
        if (idx !== -1) {
            orders[idx].status = status;
            localStorage.setItem('bb_orders', JSON.stringify(orders));
        }
    }
};

// UI Notifications helper
function showNotification(alertId, type, text) {
    const alertEl = document.getElementById(alertId);
    if (!alertEl) return;
    
    alertEl.textContent = text;
    alertEl.className = `alert alert-${type}`;
    alertEl.style.display = 'block';
    
    setTimeout(() => {
        alertEl.style.display = 'none';
    }, 6000);
}
