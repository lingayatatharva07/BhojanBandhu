package com.bhojanbandhu.platform.order;

import com.bhojanbandhu.platform.common.DomainEventPublisher;
import com.bhojanbandhu.platform.common.Enums.OrderStatus;
import com.bhojanbandhu.platform.menu.Meal;
import com.bhojanbandhu.platform.menu.MealRepository;
import com.bhojanbandhu.platform.payment.Payment;
import com.bhojanbandhu.platform.payment.PaymentRepository;
import com.bhojanbandhu.platform.provider.Provider;
import com.bhojanbandhu.platform.provider.ProviderRepository;
import com.bhojanbandhu.platform.user.User;
import com.bhojanbandhu.platform.user.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static com.bhojanbandhu.platform.common.Enums.PaymentStatus.PAID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final CustomerOrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderTrackingRepository tracking;
    private final PaymentRepository payments;
    private final UserRepository users;
    private final ProviderRepository providers;
    private final MealRepository meals;
    private final DomainEventPublisher events;

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.secret}")
    private String keySecret;

    @PostMapping
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public OrderSummary placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        User customer = users.findById(request.customerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        Provider provider = providers.findById(request.providerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Provider not found"));

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setProvider(provider);
        order.setStatus(OrderStatus.PLACED);
        CustomerOrder savedOrder = orders.save(order);

        BigDecimal total = BigDecimal.ZERO;
        for (OrderLineRequest line : request.items()) {
            Meal meal = meals.findById(line.mealId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meal not found: " + line.mealId()));
            if (!meal.isAvailable()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Meal is not available: " + line.mealId());
            }
            BigDecimal lineTotal = meal.getPrice().multiply(BigDecimal.valueOf(line.quantity()));
            OrderItem item = new OrderItem();
            item.setOrder(savedOrder);
            item.setMeal(meal);
            item.setQuantity(line.quantity());
            item.setPrice(meal.getPrice());
            item.setTotal(lineTotal);
            orderItems.save(item);
            total = total.add(lineTotal);
        }

        savedOrder.setTotalAmount(total);
        Payment payment = new Payment();
        payment.setUser(customer);
        payment.setOrder(savedOrder);
        payment.setAmount(total);
        payment.setMethod(request.paymentMethod());

        if ("RAZORPAY".equalsIgnoreCase(request.paymentMethod())) {
            try {
                org.json.JSONObject options = new org.json.JSONObject();
                options.put("razorpay_order_id", request.razorpayOrderId());
                options.put("razorpay_payment_id", request.razorpayPaymentId());
                options.put("razorpay_signature", request.razorpaySignature());

                boolean isValid = com.razorpay.Utils.verifyPaymentSignature(options, keySecret);
                if (!isValid) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment signature verification failed");
                }
                payment.setStatus(PAID);
                payment.setPaidAt(Instant.now());
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment verification error: " + e.getMessage());
            }
        } else {
            payment.setStatus(PAID);
            payment.setPaidAt(Instant.now());
        }

        payments.save(payment);

        addTracking(savedOrder, OrderStatus.PLACED, "Order placed and payment captured");
        events.publish("order.placed", "{\"orderId\":" + savedOrder.getId() + "}");
        return OrderSummary.from(savedOrder);
    }

    @PatchMapping("/{orderId}/status")
    @Transactional
    public OrderSummary updateStatus(@PathVariable Long orderId, @Valid @RequestBody UpdateOrderStatusRequest request) {
        CustomerOrder order = orders.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setStatus(request.status());
        addTracking(order, request.status(), request.description());
        events.publish("delivery.status.changed", "{\"orderId\":" + order.getId() + ",\"status\":\"" + request.status() + "\"}");
        return OrderSummary.from(order);
    }

    private void addTracking(CustomerOrder order, OrderStatus status, String description) {
        OrderTracking orderTracking = new OrderTracking();
        orderTracking.setOrder(order);
        orderTracking.setStatus(status);
        orderTracking.setDescription(description);
        tracking.save(orderTracking);
    }

    @GetMapping("/customer/{customerId}")
    @Transactional
    public List<OrderDetailsResponse> getCustomerOrders(@PathVariable Long customerId) {
        return orders.findByCustomerId(customerId).stream()
                .map(order -> {
                    List<OrderItem> items = orderItems.findByOrderId(order.getId());
                    return OrderDetailsResponse.from(order, items);
                })
                .toList();
    }

    @GetMapping("/provider/{providerId}")
    @Transactional
    public List<OrderDetailsResponse> getProviderOrders(@PathVariable Long providerId) {
        return orders.findByProviderId(providerId).stream()
                .map(order -> {
                    List<OrderItem> items = orderItems.findByOrderId(order.getId());
                    return OrderDetailsResponse.from(order, items);
                })
                .toList();
    }

    @GetMapping("/{orderId}")
    @Transactional
    public OrderDetailsResponse getOrderDetails(@PathVariable Long orderId) {
        CustomerOrder order = orders.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        List<OrderItem> items = orderItems.findByOrderId(orderId);
        return OrderDetailsResponse.from(order, items);
    }

    public record PlaceOrderRequest(
            @NotNull Long customerId,
            @NotNull Long providerId,
            @NotEmpty List<OrderLineRequest> items,
            String paymentMethod,
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature
    ) {
    }

    public record OrderLineRequest(@NotNull Long mealId, @Min(1) int quantity) {
    }

    public record UpdateOrderStatusRequest(@NotNull OrderStatus status, String description) {
    }

    public record OrderSummary(Long id, Long customerId, Long providerId, BigDecimal totalAmount, OrderStatus status) {
        static OrderSummary from(CustomerOrder order) {
            return new OrderSummary(order.getId(), order.getCustomer().getId(), order.getProvider().getId(), order.getTotalAmount(), order.getStatus());
        }
    }

    public record OrderDetailsResponse(
            Long id,
            Long customerId,
            String customerName,
            Long providerId,
            String providerName,
            BigDecimal totalAmount,
            OrderStatus status,
            List<OrderItemDetails> items
    ) {
        public static OrderDetailsResponse from(CustomerOrder order, List<OrderItem> items) {
            return new OrderDetailsResponse(
                    order.getId(),
                    order.getCustomer().getId(),
                    order.getCustomer().getName(),
                    order.getProvider().getId(),
                    order.getProvider().getOrgName(),
                    order.getTotalAmount(),
                    order.getStatus(),
                    items.stream().map(OrderItemDetails::from).toList()
            );
        }
    }

    public record OrderItemDetails(Long mealId, String mealName, int quantity, BigDecimal price, BigDecimal total) {
        static OrderItemDetails from(OrderItem item) {
            return new OrderItemDetails(
                    item.getMeal().getId(),
                    item.getMeal().getName(),
                    item.getQuantity(),
                    item.getPrice(),
                    item.getTotal()
            );
        }
    }
}
