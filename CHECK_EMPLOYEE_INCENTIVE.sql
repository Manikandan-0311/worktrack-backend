-- ============================================================================
-- CHECK EMPLOYEE INCENTIVE AND ACTIVE STATUS
-- ============================================================================

-- Check all 6 employees from the submissions query
SELECT 
    e.employee_id,
    e.first_name || ' ' || COALESCE(e.last_name, '') as employee_name,
    e.is_active,
    e.org_id,
    e.salary,
    e.incentive
FROM base.employee e
WHERE e.employee_id IN (39, 40, 41, 42, 43, 44)
ORDER BY e.employee_id;

-- ============================================================================
-- Count total working days in calendar (is_active = false)
SELECT 
    COUNT(*) as total_working_days
FROM base.calendar c
WHERE c.org_id = 1 
    AND c.calendar_date >= '2026-01-01'::date 
    AND c.calendar_date < '2026-01-31'::date
    AND c.is_active = false;

-- ============================================================================
-- Show the complete calculation for all 6 employees
SELECT 
    e.employee_id,
    e.first_name || ' ' || COALESCE(e.last_name, '') as employee_name,
    e.is_active,
    e.incentive,
    
    (SELECT COUNT(*) FROM base.calendar c
     WHERE c.org_id = 1 
       AND c.calendar_date >= '2026-01-01'::date
       AND c.calendar_date < '2026-01-31'::date
       AND c.is_active = false) as org_total_working_days,
    
    (SELECT COUNT(DISTINCT s.question_date) 
     FROM compliance.compliance_submission s
     WHERE s.employee_id = e.employee_id
       AND s.question_date >= '2026-01-01'::date
       AND s.question_date < '2026-01-31'::date
       AND s.is_active = true) as employee_working_days,
    
    ROUND(
        CAST(e.incentive AS NUMERIC(10,2)) / 
        CAST((SELECT COUNT(*) FROM base.calendar c
              WHERE c.org_id = 1 
                AND c.calendar_date >= '2026-01-01'::date
                AND c.calendar_date < '2026-01-31'::date
                AND c.is_active = false) AS NUMERIC(10,2)) * 
        CAST((SELECT COUNT(DISTINCT s.question_date) 
              FROM compliance.compliance_submission s
              WHERE s.employee_id = e.employee_id
                AND s.question_date >= '2026-01-01'::date
                AND s.question_date < '2026-01-31'::date
                AND s.is_active = true) AS NUMERIC(10,2)),
        2
    ) as calculated_incentive
    
FROM base.employee e
WHERE e.employee_id IN (39, 40, 41, 42, 43, 44)
ORDER BY e.employee_id;
