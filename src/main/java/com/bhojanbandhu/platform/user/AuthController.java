package com.bhojanbandhu.platform.user;

import com.bhojanbandhu.platform.common.Enums.RoleName;
import com.bhojanbandhu.platform.common.Enums.UserStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummary register(@Valid @RequestBody RegisterRequest request) {
        if (users.existsByEmail(request.email()) || users.existsByPhone(request.phone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or phone already registered");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setStatus(UserStatus.ACTIVE);
        return UserSummary.from(users.save(user));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = users.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return new LoginResponse(UserSummary.from(user), "local-dev-token-" + user.getId());
    }

    @GetMapping("/me")
    public String me() {
        return "JWT authentication is not enabled yet. Use /api/v1/auth/login for local testing.";
    }

    public record RegisterRequest(
            @NotBlank String name,
            @Email String email,
            @NotBlank String phone,
            @NotBlank String password,
            RoleName role
    ) {
        public RegisterRequest {
            if (role == null) {
                role = RoleName.CUSTOMER;
            }
        }
    }

    public record LoginRequest(@Email String email, @NotBlank String password) {
    }

    public record LoginResponse(UserSummary user, String token) {
    }

    public record UserSummary(Long id, String name, String email, String phone, RoleName role, UserStatus status) {
        static UserSummary from(User user) {
            return new UserSummary(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole(), user.getStatus());
        }
    }
}
