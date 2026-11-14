package com.aiscalendar.user;

import com.aiscalendar.security.UserPrincipal;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

  @GetMapping
  public Map<String, Object> me(@AuthenticationPrincipal UserPrincipal principal) {
    return Map.of(
        "userId", principal.getId(),
        "email", principal.getUsername(),
        "displayName", principal.getDisplayName());
  }
}
