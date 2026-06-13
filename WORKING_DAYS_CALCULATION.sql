-- ============================================================================
-- WORKING DAYS CALCULATION - CORRECTED LOGIC
-- ============================================================================
-- 
-- Formula: WORKING DAYS = TOTAL CALENDAR DAYS - HOLIDAY COUNT
--
-- Explanation:
-- - Total days in range = number of days between fromDate and toDate
-- - Holiday count = records where calendar.is_active = true
-- - Working days = Total days - Holiday count (all unmarked days are working days)
--
-- Example:
-- - Date range: 2026-01-01 to 2026-01-31 (31 days total)
-- - Holidays marked in calendar (is_active = true): 5 days
-- - Working days = 31 - 5 = 26 days
--
-- ============================================================================

-- Step 1: Get total days in the range
SELECT 
    CAST('2026-01-31'::date - '2026-01-01'::date AS INT) as total_days;

-- Step 2: Get holiday count (is_active = true)
SELECT 
    COUNT(*) as holiday_count
FROM base.calendar c
WHERE c.org_id = 1
    AND c.calendar_date >= '2026-01-01'::date
    AND c.calendar_date < '2026-01-31'::date
    AND c.is_active = true;  -- TRUE = Holiday

-- Step 3: Calculate working days (TOTAL - HOLIDAYS)
SELECT 
    CAST('2026-01-31'::date - '2026-01-01'::date AS INT) as total_days,
    (SELECT COUNT(*) FROM base.calendar c
     WHERE c.org_id = 1
       AND c.calendar_date >= '2026-01-01'::date
       AND c.calendar_date < '2026-01-31'::date
       AND c.is_active = true) as holiday_count,
    CAST('2026-01-31'::date - '2026-01-01'::date AS INT) - 
    (SELECT COUNT(*) FROM base.calendar c
     WHERE c.org_id = 1
       AND c.calendar_date >= '2026-01-01'::date
       AND c.calendar_date < '2026-01-31'::date
       AND c.is_active = true) as working_days;

-- Step 4: View all calendar entries for org 1 in the date range
SELECT 
    c.calendar_id,
    c.calendar_date,
    c.is_active as is_holiday,
    c.remarks
FROM base.calendar c
WHERE c.org_id = 1
    AND c.calendar_date >= '2026-01-01'::date
    AND c.calendar_date < '2026-01-31'::date
ORDER BY c.calendar_date;

-- ============================================================================
-- COMPLETE EMPLOYEE INCENTIVE CALCULATION (WITH CORRECTED WORKING DAYS)
-- ============================================================================

SELECT 
    e.employee_id,
    e.first_name || ' ' || COALESCE(e.last_name, '') as employee_name,
    e.is_active,
    e.incentive as total_incentive,
    
    -- Total working days in organization (CORRECTED FORMULA)
    CAST('2026-01-31'::date - '2026-01-01'::date AS INT) - 
    (SELECT COUNT(*) FROM base.calendar c
     WHERE c.org_id = 1 
       AND c.calendar_date >= '2026-01-01'::date
       AND c.calendar_date < '2026-01-31'::date
       AND c.is_active = true) as org_total_working_days,
    
    -- Employee's working days (with submissions)
    (SELECT COUNT(DISTINCT s.question_date) 
     FROM compliance.compliance_submission s
     WHERE s.employee_id = e.employee_id
       AND s.question_date >= '2026-01-01'::date
       AND s.question_date < '2026-01-31'::date
       AND s.is_active = true) as employee_working_days,
    
    -- Calculated incentive
    ROUND(
        CAST(e.incentive AS NUMERIC(10,2)) / 
        CAST((
            CAST('2026-01-31'::date - '2026-01-01'::date AS INT) - 
            (SELECT COUNT(*) FROM base.calendar c
             WHERE c.org_id = 1 
               AND c.calendar_date >= '2026-01-01'::date
               AND c.calendar_date < '2026-01-31'::date
               AND c.is_active = true)
        ) AS NUMERIC(10,2)) * 
        CAST((SELECT COUNT(DISTINCT s.question_date) 
              FROM compliance.compliance_submission s
              WHERE s.employee_id = e.employee_id
                AND s.question_date >= '2026-01-01'::date
                AND s.question_date < '2026-01-31'::date
                AND s.is_active = true) AS NUMERIC(10,2)),
        2
    ) as calculated_incentive
    
FROM base.employee e
WHERE e.org_id = 1 
    AND e.is_active = true
    AND e.incentive > 0
    AND (SELECT COUNT(DISTINCT s.question_date) 
         FROM compliance.compliance_submission s
         WHERE s.employee_id = e.employee_id
           AND s.question_date >= '2026-01-01'::date
           AND s.question_date < '2026-01-31'::date
           AND s.is_active = true) > 0
ORDER BY e.employee_id;
