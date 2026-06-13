package com.spearhead.ufc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.spearhead.ufc.model.Calendar;

public interface CalendarRepository extends JpaRepository<Calendar, Integer> {
	Optional<Calendar> findByOrgOrgIdAndCalendarDate(Integer orgId, LocalDate calendarDate);
	List<Calendar> findAllByOrgOrgId(Integer orgId);
	
	/**
	 * Count holidays in the date range (is_active = true means holiday)
	 */
	@Query(value = "SELECT COUNT(*) FROM base.calendar c " +
			"WHERE c.org_id = :orgId AND c.calendar_date >= :startDate AND c.calendar_date <= :endDate " +
			"AND c.is_active = true",
			nativeQuery = true)
	long countHolidaysInRange(@Param("orgId") Integer orgId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	/**
	 * Count working days (total days - holidays)
	 * Working days = Total calendar days - Holiday days (is_active = true)
	 * is_active = true = Holiday
	 * is_active = false = Working day
	 * Days not marked in calendar = Working days
	 */
	@Query(value = "SELECT " +
			"(SELECT CAST((CAST(:endDate AS DATE) - CAST(:startDate AS DATE)) AS INT)) - " +
			"(SELECT COUNT(*) FROM base.calendar c " +
			"WHERE c.org_id = :orgId AND c.calendar_date >= :startDate AND c.calendar_date < :endDate " +
			"AND c.is_active = true)",
			nativeQuery = true)
	long countWorkingDays(@Param("orgId") Integer orgId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);
}

