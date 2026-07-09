package com.bhojanbandhu.platform.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MealRepository extends JpaRepository<Meal, Long> {
    List<Meal> findByProviderIdAndAvailableTrue(Long providerId);
}
