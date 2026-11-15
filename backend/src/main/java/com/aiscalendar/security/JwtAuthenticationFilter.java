package com.aiscalendar.security;

import com.aiscalendar.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs once per HTTP request. If a valid Bearer JWT is present, loads the user and stores
 * authentication in {@link SecurityContextHolder} so controllers see the current user.
 *
 * <p>Missing/invalid tokens are ignored here — later authorization still returns 401 for protected
 * routes. That way public routes like {@code /api/auth/login} keep working.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserRepository userRepository;

  public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
    this.jwtService = jwtService;
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = header.substring(7); // strip "Bearer "
    if (jwtService.isValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
      UUID userId = jwtService.extractUserId(token);
      // Re-load user from DB so deleted/disabled accounts don't keep working on old tokens.
      userRepository
          .findById(userId)
          .ifPresent(
              user -> {
                UserPrincipal principal = new UserPrincipal(user);
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
              });
    }

    filterChain.doFilter(request, response);
  }
}
