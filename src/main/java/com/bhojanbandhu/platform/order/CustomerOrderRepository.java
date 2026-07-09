package com.bhojanbandhu.platform.order;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByCustomerId(Long customerId);
    List<CustomerOrder> findByProviderId(Long providerId);
}
