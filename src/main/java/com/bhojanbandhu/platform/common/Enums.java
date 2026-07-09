package com.bhojanbandhu.platform.common;

public final class Enums {
    private Enums() {
    }

    public enum RoleName { CUSTOMER, PROVIDER, ADMIN }
    public enum UserStatus { ACTIVE, BLOCKED, PENDING_VERIFICATION }
    public enum AddressType { HOME, OFFICE, OTHER }
    public enum ProviderStatus { PENDING, ACTIVE, SUSPENDED }
    public enum MealType { BREAKFAST, LUNCH, DINNER }
    public enum SubscriptionStatus { ACTIVE, PAUSED, CANCELLED, EXPIRED }
    public enum OrderType { ONE_TIME, SUBSCRIPTION_MEAL }
    public enum OrderStatus { PLACED, CONFIRMED, PREPARING, OUT_FOR_DELIVERY, DELIVERED, CANCELLED }
    public enum PaymentStatus { CREATED, PAID, FAILED, REFUNDED }
    public enum NotificationType { EMAIL, PUSH, SMS, IN_APP }
}
