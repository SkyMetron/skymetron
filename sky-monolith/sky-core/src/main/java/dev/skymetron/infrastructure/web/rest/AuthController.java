package dev.skymetron.infrastructure.web.rest;

import dev.skymetron.infrastructure.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication — JWT login and user info")
public class AuthController {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthController(UserDetailsService userDetailsService,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider tokenProvider) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Operation(summary = "Login", description = "Authenticate with username/password, returns JWT token")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        UserDetails user = userDetailsService.loadUserByUsername(request.username());
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        List<String> roles = user.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .toList();
        String token = tokenProvider.createToken(user.getUsername(), roles);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", user.getUsername(),
                "roles", roles
        ));
    }

    @Operation(summary = "Current user", description = "Returns authenticated username from JWT token")
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestAttribute(required = false) String username) {
        return ResponseEntity.ok(Map.of("username", username));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
}
