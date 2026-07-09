package com.bhojanbandhu.platform.subscription;

import com.bhojanbandhu.platform.common.DomainEventPublisher;
import com.bhojanbandhu.platform.provider.ProviderRepository;
import com.bhojanbandhu.platform.user.User;
import com.bhojanbandhu.platform.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SubscriptionController {
    private final SubscriptionPlanRepository plans;
    private final SubscriptionRepository subscriptions;
    private final ProviderRepository providers;
    private final UserRepository users;
    private final DomainEventPublisher events;

    @GetMapping("/catalog/providers/{providerId}/plans")
    @Transactional(readOnly = true)
    public List<PlanSummary> providerPlans(@PathVariable Long providerId) {
        return plans.findByProviderIdAndActiveTrue(providerId).stream().map(PlanSummary::from).toList();
    }

    @PostMapping("/providers/{providerId}/plans")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanSummary createPlan(@PathVariable Long providerId, @Valid @RequestBody CreatePlanRequest request) {
        var provider = providers.findById(providerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Provider not found"));

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setProvider(provider);
        plan.setName(request.name());
        plan.setMealType(request.mealType());
        plan.setPrice(request.price());
        plan.setDurationDays(request.durationDays());
        plan.setDescription(request.description());
        plan.setActive(true);
        return PlanSummary.from(plans.save(plan));
    }

    @PostMapping("/subscriptions")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionSummary createSubscription(@Valid @RequestBody CreateSubscriptionRequest request) {
        User customer = users.findById(request.customerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
        SubscriptionPlan plan = plans.findById(request.planId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));

        LocalDate startDate = request.startDate() == null ? LocalDate.now() : request.startDate();
        Subscription subscription = new Subscription();
        subscription.setCustomer(customer);
        subscription.setProvider(plan.getProvider());
        subscription.setPlan(plan);
        subscription.setStartDate(startDate);
        subscription.setEndDate(startDate.plusDays(plan.getDurationDays() - 1L));
        subscription.setAmount(plan.getPrice());
        Subscription saved = subscriptions.save(subscription);
        events.publish("subscription.created", "{\"subscriptionId\":" + saved.getId() + "}");
        return SubscriptionSummary.from(saved);
    }

    @GetMapping("/subscriptions/customer/{customerId}")
    @Transactional(readOnly = true)
    public List<SubscriptionSummary> customerSubscriptions(@PathVariable Long customerId) {
        return subscriptions.findByCustomerId(customerId).stream().map(SubscriptionSummary::from).toList();
    }

    @GetMapping("/subscriptions/provider/{providerId}")
    @Transactional(readOnly = true)
    public List<SubscriptionSummary> providerSubscriptions(@PathVariable Long providerId) {
        return subscriptions.findByProviderId(providerId).stream().map(SubscriptionSummary::from).toList();
    }

    public record CreatePlanRequest(@NotNull com.bhojanbandhu.platform.common.Enums.MealType mealType, String name, @NotNull BigDecimal price, @NotNull Integer durationDays, String description) {
    }

    public record CreateSubscriptionRequest(@NotNull Long customerId, @NotNull Long planId, LocalDate startDate) {
    }

    public record PlanSummary(Long id, Long providerId, String name, com.bhojanbandhu.platform.common.Enums.MealType mealType, BigDecimal price, Integer durationDays, String description) {
        static PlanSummary from(SubscriptionPlan plan) {
            return new PlanSummary(plan.getId(), plan.getProvider().getId(), plan.getName(), plan.getMealType(), plan.getPrice(), plan.getDurationDays(), plan.getDescription());
        }
    }

    public record SubscriptionSummary(Long id, Long customerId, Long providerId, Long planId, LocalDate startDate, LocalDate endDate, BigDecimal amount, String status) {
        static SubscriptionSummary from(Subscription subscription) {
            return new SubscriptionSummary(
                    subscription.getId(),
                    subscription.getCustomer().getId(),
                    subscription.getProvider().getId(),
                    subscription.getPlan().getId(),
                    subscription.getStartDate(),
                    subscription.getEndDate(),
                    subscription.getAmount(),
                    subscription.getStatus().name());
        }
    }
}
