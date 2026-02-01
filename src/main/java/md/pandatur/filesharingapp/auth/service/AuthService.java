package md.pandatur.filesharingapp.auth.service;

import lombok.RequiredArgsConstructor;
import md.pandatur.filesharingapp.auth.dto.AuthResponse;
import md.pandatur.filesharingapp.auth.dto.LoginRequest;
import md.pandatur.filesharingapp.auth.dto.RegisterRequest;
import md.pandatur.filesharingapp.auth.model.User;
import md.pandatur.filesharingapp.auth.repository.UserRepository;
import md.pandatur.filesharingapp.auth.security.JwtUtil;
import md.pandatur.filesharingapp.auth.security.UserDetailsImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthResponse register(final RegisterRequest request) {
        if (this.userRepository.existsByUsername(request.username())) {
            throw new RuntimeException("Username is already taken");
        }

        final var user = User.builder()
                .name(request.name())
                .surname(request.surname())
                .username(request.username())
                .password(this.passwordEncoder.encode(request.password()))
                .build();

        this.userRepository.save(user);

        final UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        final String token = this.jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .id(user.getId())
                .token(token)
                .username(userDetails.getUsername())
                .name(userDetails.getName())
                .surname(userDetails.getSurname())
                .build();
    }

    public AuthResponse login(final LoginRequest request) {
        final Authentication authentication = this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        final UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        if (userDetails == null) {
            throw new RuntimeException("Failed to authenticate user");
        }
        final String token = this.jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .id(userDetails.getId())
                .token(token)
                .username(userDetails.getUsername())
                .name(userDetails.getName())
                .surname(userDetails.getSurname())
                .build();
    }
}
