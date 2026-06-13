-- ============================================================================
-- DEBUG QUERIES FOR EMPLOYEE INCENTIVE CALCULATION
-- ============================================================================

-- STEP 1: Check all active employees in organization (orgId = 1)
-- This should show all active employees
SELECT 
    e.employee_id,
    e.first_name || ' ' || COALESCE(e.last_name, '') as employee_name,
    e.incentive,
    e.is_active,
    e.org_id
FROM base.employee e
WHERE e.org_id = 1 AND e.is_active = true
ORDER BY e.employee_id;

-- ============================================================================
-- STEP 2: Check total working days in organization (is_active = false)
-- Working Day Definition: is_active = false
-- Calendar Date Range: 2026-01-01 to 2026-01-31
SELECT 
    COUNT(*) as total_working_days
FROM base.calendar c
WHERE c.org_id = 1 
    AND c.calendar_date >= '2026-01-01'::date 
    AND c.calendar_date < '2026-01-31'::date
    AND c.is_active = false;  -- false = working day, true = holiday

-- ============================================================================
-- STEP 3: Check submissions for ALL employees
-- This shows which employees have submissions in the date range
SELECT 
    s.employee_id,
    e.first_name || ' ' || COALESCE(e.last_name, '') as employee_name,
    COUNT(DISTINCT s.question_date) as distinct_working_days,
    COUNT(*) as total_submissions,
    MIN(s.question_date) as first_submission_date,
    MAX(s.question_date) as last_submission_date
FROM compliance.compliance_submission s
JOIN base.employee e ON s.employee_id = e.employee_id
WHERE e.org_id = 1
    AND s.question_date >= '2026-01-01'::date
    AND s.question_date < '2026-01-31'::date
    AND s.is_active = true  -- Only active submissions
GROUP BY s.employee_id, e.first_name, e.last_name
ORDER BY s.employee_id;

-- ============================================================================
-- STEP 4: Check detailed submissions per employee
-- This shows all individual submissions
SELECT 
    s.submission_id,
    s.employee_id,
    e.first_name || ' ' || COALESCE(e.last_name, '') as employee_name,
    s.question_date,
    s.question_id,
    s.is_active,
    s.marks_awarded,
    s.created_dt,
    s.submitted_at
FROM compliance.compliance_submission s
JOIN base.employee e ON s.employee_id = e.employee_id
WHERE e.org_id = 1
    AND s.question_date >= '2026-01-01'::date
    AND s.question_date < '2026-01-31'::date
ORDER BY s.employee_id, s.question_date, s.submission_id;

-- ============================================================================
-- STEP 5: Check which employees have incentive > 0
-- Only these employees should be included in calculation
SELECT 
    e.employee_id,
    e.first_name || ' ' || COALESCE(e.last_name, '') as employee_name,
    e.salary,
    e.incentive,
    e.is_active
FROM base.employee e
WHERE e.org_id = 1 
    AND e.is_active = true
    AND e.incentive > 0
ORDER BY e.employee_id;

-- ============================================================================
-- STEP 6: Complete calculation per employee (MANUAL VERIFICATION)
-- This shows the complete calculation logic
SELECT 
    e.employee_id,
    e.first_name || ' ' || COALESCE(e.last_name, '') as employee_name,
    e.incentive as total_incentive,
    
    -- Total working days in organization
    (SELECT COUNT(*) FROM base.calendar c
     WHERE c.org_id = 1 
       AND c.calendar_date >= '2026-01-01'::date
       AND c.calendar_date < '2026-01-31'::date
       AND c.is_active = false) as org_total_working_days,
    
    -- Employee's working days (with submissions)
    (SELECT COUNT(DISTINCT s.question_date) 
     FROM compliance.compliance_submission s
     WHERE s.employee_id = e.employee_id
       AND s.question_date >= '2026-01-01'::date
       AND s.question_date < '2026-01-31'::date
       AND s.is_active = true) as employee_working_days,
    
    -- Calculated incentive
    ROUND(
        CAST(e.incentive AS DECIMAL(10,2)) / 
        CAST((SELECT COUNT(*) FROM base.calendar c
              WHERE c.org_id = 1 
                AND c.calendar_date >= '2026-01-01'::date
                AND c.calendar_date < '2026-01-31'::date
                AND c.is_active = false) AS DECIMAL(10,2)) * 
        CAST((SELECT COUNT(DISTINCT s.question_date) 
              FROM compliance.compliance_submission s
              WHERE s.employee_id = e.employee_id
                AND s.question_date >= '2026-01-01'::date
                AND s.question_date < '2026-01-31'::date
                AND s.is_active = true) AS DECIMAL(10,2)),
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

-- ============================================================================
-- IMPORTANT NOTES:
-- ============================================================================
-- 1. Date Range: 2026-01-01 to 2026-01-31 (can change as needed)
-- 2. Organization ID: 1 (change as needed)
-- 3. Working Day Definition: calendar.is_active = false
--    - FALSE = Working Day
--    - TRUE = Holiday/Non-working day
-- 4. Submission must have: is_active = true
-- 5. Only active employees (is_active = true) are included
-- 6. Only employees with incentive > 0 are included
-- 7. Only employees with submissions are included

