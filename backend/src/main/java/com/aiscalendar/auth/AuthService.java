package com.aiscalendar.auth;

import com.aiscalendar.auth.dto.AuthResponse;
import com.aiscalendar.auth.dto.LoginRequest;
import com.aiscalendar.auth.dto.SignupRequest;
import com.aiscalendar.common.ApiException;
import com.aiscalendar.security.JwtService;
import com.aiscalendar.security.UserPrincipal;
import com.aiscalendar.user.User;
import com.aiscalendar.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      AuthenticationManager authenticationManager) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.authenticationManager = authenticationManager;
  }

  @Transactional
  public AuthResponse signup(SignupRequest request) {
    String email = request.email().trim().toLowerCase();
    if (userRepository.existsByEmailIgnoreCase(email)) {
      throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
    }

    User user =
        new User(
            email,
            request.displayName().trim(),
            passwordEncoder.encode(request.password()));
    userRepository.save(user);

    String token = jwtService.generateToken(user.getId(), user.getEmail());
    return new AuthResponse(token, user.getId(), user.getEmail(), user.getDisplayName());
  }

  public AuthResponse login(LoginRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.email().trim().toLowerCase(), request.password()));
    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    String token = jwtService.generateToken(principal.getId(), principal.getUsername());
    return new AuthResponse(
        token, principal.getId(), principal.getUsername(), principal.getDisplayName());
  }
}
