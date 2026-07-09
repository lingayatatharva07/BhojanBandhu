package com.bhojanbandhu.platform.provider;

import com.bhojanbandhu.platform.common.Enums.ProviderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProviderRepository extends JpaRepository<Provider, Long> {
    List<Provider> findByStatus(ProviderStatus status);
}
