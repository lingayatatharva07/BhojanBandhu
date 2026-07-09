package com.bhojanbandhu.platform.provider;

import com.bhojanbandhu.platform.common.Enums.ProviderStatus;
import com.bhojanbandhu.platform.user.User;
import com.bhojanbandhu.platform.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ProviderController {
    private final ProviderRepository providers;
    private final UserRepository users;

    @GetMapping
    public List<ProviderSummary> list() {
        return providers.findByStatus(ProviderStatus.ACTIVE).stream().map(ProviderSummary::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProviderSummary create(@Valid @RequestBody CreateProviderRequest request) {
        User owner = users.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Provider user not found"));

        Provider provider = new Provider();
        provider.setUser(owner);
        provider.setOrgName(request.orgName());
        provider.setOwnerName(request.ownerName());
        provider.setDescription(request.description());
        provider.setStatus(ProviderStatus.ACTIVE);
        return ProviderSummary.from(providers.save(provider));
    }

    public record CreateProviderRequest(@NotNull Long userId, @NotBlank String orgName, @NotBlank String ownerName, String description) {
    }

    public record ProviderSummary(Long id, Long userId, String orgName, String ownerName, String description, Double rating, ProviderStatus status) {
        static ProviderSummary from(Provider provider) {
            return new ProviderSummary(provider.getId(), provider.getUser().getId(), provider.getOrgName(), provider.getOwnerName(), provider.getDescription(), provider.getRating(), provider.getStatus());
        }
    }
}
