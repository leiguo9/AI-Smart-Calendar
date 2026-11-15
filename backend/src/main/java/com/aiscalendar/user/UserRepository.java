package com.aiscalendar.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data derives queries from method names ({@code IgnoreCase} → case-insensitive email). */
public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);
}
