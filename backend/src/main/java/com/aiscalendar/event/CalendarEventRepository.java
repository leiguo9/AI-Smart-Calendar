package com.aiscalendar.event;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository — method names / {@link Query} become SQL at runtime.
 *
 * <p>No implementation class needed; Spring generates a proxy from this interface.
 */
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {

  /**
   * Events that overlap {@code [rangeStart, rangeEnd)} for one owner.
   *
   * <p>Standard interval overlap: start &lt; rangeEnd AND end &gt; rangeStart.
   */
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

  /** Ownership check baked into the query — returns empty if id exists but belongs to someone else. */
  Optional<CalendarEvent> findByIdAndOwnerId(UUID id, UUID ownerId);
}
