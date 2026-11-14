package com.aiscalendar.event;

import com.aiscalendar.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
      boolean allDay) {
    this.owner = owner;
    this.title = title;
    this.description = description;
    this.startAt = startAt;
    this.endAt = endAt;
    this.allDay = allDay;
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

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void update(
      String title, String description, Instant startAt, Instant endAt, boolean allDay) {
    this.title = title;
    this.description = description;
    this.startAt = startAt;
    this.endAt = endAt;
    this.allDay = allDay;
    this.updatedAt = Instant.now();
  }
}
