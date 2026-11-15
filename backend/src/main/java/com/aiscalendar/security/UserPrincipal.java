package com.aiscalendar.security;

import com.aiscalendar.user.User;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security's view of the logged-in user (adapter around our {@link User} entity).
 *
 * <p>{@link #getUsername()} returns email because that's what we authenticate with.
 */
public class UserPrincipal implements UserDetails {

  private final UUID id;
  private final String email;
  private final String displayName;
  private final String passwordHash;

  public UserPrincipal(User user) {
    this.id = user.getId();
    this.email = user.getEmail();
    this.displayName = user.getDisplayName();
    this.passwordHash = user.getPasswordHash();
  }

  public UUID getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    // Single-role app for now; expand later for admin/etc.
    return List.of(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
