package com.bhojanbandhu.platform.menu;

import com.bhojanbandhu.platform.common.DomainEventPublisher;
import com.bhojanbandhu.platform.common.Enums.MealType;
import com.bhojanbandhu.platform.provider.Provider;
import com.bhojanbandhu.platform.provider.ProviderRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MenuController {
    private final MealRepository meals;
    private final ProviderRepository providers;
    private final DomainEventPublisher events;

    @GetMapping("/catalog/providers/{providerId}/meals")
    @Transactional(readOnly = true)
    public List<MealSummary> providerMeals(@PathVariable Long providerId) {
        return meals.findByProviderIdAndAvailableTrue(providerId).stream().map(MealSummary::from).toList();
    }

    @PostMapping("/providers/{providerId}/meals")
    @ResponseStatus(HttpStatus.CREATED)
    public MealSummary createMeal(@PathVariable Long providerId, @Valid @RequestBody CreateMealRequest request) {
        Provider provider = providers.findById(providerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Provider not found"));

        Meal meal = new Meal();
        meal.setProvider(provider);
        meal.setMealType(request.mealType());
        meal.setName(request.name());
        meal.setTitle(request.title());
        meal.setDescription(request.description());
        meal.setPrice(request.price());
        meal.setTotalCalories(request.totalCalories());
        meal.setImageUrl(request.imageUrl());
        meal.setAvailable(true);
        Meal saved = meals.save(meal);
        events.publish("menu.updated", "{\"providerId\":" + providerId + ",\"mealId\":" + saved.getId() + "}");
        return MealSummary.from(saved);
    }

    public record CreateMealRequest(@NotNull MealType mealType, @NotBlank String name, String title, String description, @NotNull BigDecimal price, Integer totalCalories, String imageUrl) {
    }

    public record MealSummary(Long id, Long providerId, MealType mealType, String name, String title, String description, BigDecimal price, Integer totalCalories, String imageUrl) {
        static MealSummary from(Meal meal) {
            return new MealSummary(meal.getId(), meal.getProvider().getId(), meal.getMealType(), meal.getName(), meal.getTitle(), meal.getDescription(), meal.getPrice(), meal.getTotalCalories(), meal.getImageUrl());
        }
    }
}
