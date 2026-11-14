package com.aiscalendar.event;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {

  @Query(
      """
      SELECT e FROM CalendarEvent e
      WHERE e.owner.id = :ownerId
        AND e.startAt < :rangeEnd
        AND e.endAt > :rangeStart
      ORDER BY e.startAt ASC
      """)
  List<CalendarEvent> findOverlapping(
      @Param("ownerId") UUID ownerId,
      @Param("rangeStart") Instant rangeStart,
      @Param("rangeEnd") Instant rangeEnd);

  Optional<CalendarEvent> findByIdAndOwnerId(UUID id, UUID ownerId);
}
