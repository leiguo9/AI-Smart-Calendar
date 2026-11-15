package com.aiscalendar.event;

import com.aiscalendar.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "calendar_events")
public class CalendarEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(length = 2000)
  private String description;

  @Column(nullable = false)
  private Instant startAt;

  @Column(nullable = false)
  private Instant endAt;

  @Column(nullable = false)
  private boolean allDay = false;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  @ColumnDefault("'OTHER'")
  private EventCategory category = EventCategory.OTHER;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  private Instant updatedAt = Instant.now();

  protected CalendarEvent() {}

  public CalendarEvent(
      User owner,
      String title,
      String description,
      Instant startAt,
      Instant endAt,
      boolean allDay,
      EventCategory category) {
    this.owner = owner;
    this.title = title;
    this.description = description;
    this.startAt = startAt;
    this.endAt = endAt;
    this.allDay = allDay;
    this.category = category == null ? EventCategory.OTHER : category;
  }

  public UUID getId() {
    return id;
  }

  public User getOwner() {
    return owner;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public Instant getStartAt() {
    return startAt;
  }

  public Instant getEndAt() {
    return endAt;
  }

  public boolean isAllDay() {
    return allDay;
  }

  public EventCategory getCategory() {
    return category;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void update(
      String title,
      String description,
      Instant startAt,
      Instant endAt,
      boolean allDay,
      EventCategory category) {
    this.title = title;
    this.description = description;
    this.startAt = startAt;
    this.endAt = endAt;
    this.allDay = allDay;
    this.category = category == null ? EventCategory.OTHER : category;
    this.updatedAt = Instant.now();
  }
}
