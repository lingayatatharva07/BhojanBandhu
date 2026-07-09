package com.bhojanbandhu.platform.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByCustomerId(Long customerId);
    List<Subscription> findByProviderId(Long providerId);
}
