# Employee Incentive Calculation API

## Overview
This API calculates employee incentive based on working days and submissions within a specified date range.

## API Endpoint
```
POST /dashboardSummary/calculate-employee-incentive
```

## Request Details

### Headers
- `Authorization`: Bearer token (required)

### Request Body (JSON)
```json
{
  "orgId": 1,
  "fromDate": "2026-01-01",
  "toDate": "2026-01-31"
}
```

### Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| orgId | Integer | Organization ID (required) |
| fromDate | String | Start date in format yyyy-MM-dd (required) |
| toDate | String | End date in format yyyy-MM-dd (required) |

## Response

### Success Response (200 OK)
```json
{
  "success": true,
  "message": "Employee incentive calculated successfully",
  "data": [
    {
      "employeeId": 1,
      "employeeName": "John Doe",
      "incentive": 15000.50,
      "workingDays": 20,
      "totalWorkingDays": 25
    },
    {
      "employeeId": 2,
      "employeeName": "Jane Smith",
      "incentive": 12500.25,
      "workingDays": 20,
      "totalWorkingDays": 25
    }
  ],
  "totalIncentiveCost": 27500.75,
  "totalEmployees": 2,
  "periodFrom": "2026-01-01",
  "periodTo": "2026-01-31"
}
```

### Error Response (400/401/500)
```json
{
  "success": false,
  "message": "Error description"
}
```

## Calculation Logic

### Step 1: Fetch Employees
- Retrieves all active employees from the specified organization
- Only considers employees where `is_active = true`

### Step 2: Count Total Working Days
- Counts working days from the **Calendar** table
- **is_active = false** = **Working Day**
- **is_active = true** = **Holiday/Non-working Day**
- Date range: fromDate to toDate

### Step 3: Count Employee Working Days
- For each employee, counts distinct days with active submissions
- Queries **compliance_submission** table where:
  - `employee_id` matches employee
  - `question_date` is between fromDate and toDate
  - `is_active = true`

### Step 4: Calculate Per-Day Incentive
```
Per-Day Incentive = Employee Total Incentive / Total Working Days
```

### Step 5: Calculate Actual Incentive Earned
```
Actual Incentive = Per-Day Incentive × Employee Working Days
```

## Database Queries Used

### Calendar Query
```sql
SELECT COUNT(*) FROM base.calendar c 
WHERE c.org_id = ? 
  AND c.calendar_date >= ? 
  AND c.calendar_date < ? 
  AND c.is_active = false  -- Working days only
```

### Submission Query
```sql
SELECT COUNT(DISTINCT s.question_date) 
FROM compliance.compliance_submission s 
WHERE s.employee_id = ? 
  AND s.question_date >= ? 
  AND s.question_date < ? 
  AND s.is_active = true
```

## Example Calculation

**Given:**
- Total Working Days in Organization: 25
- Employee "John Doe":
  - Total Incentive: 25,000
  - Working Days (with submissions): 20

**Calculation:**
```
Per-Day Incentive = 25,000 / 25 = 1,000
Actual Incentive = 1,000 × 20 = 20,000
```

## Implementation Details

### Files Modified
1. **CalendarRepository.java** - Added `countWorkingDays()` method
2. **SubmissionRepository.java** - Added `countEmployeeWorkingDays()` method
3. **DashboardSummaryService.java** - Added `calculateEmployeeIncentive()` method
4. **DashboardSummaryController.java** - Added POST endpoint `/calculate-employee-incentive`

### Key Classes Used
- `Employee` - Contains incentive, salary, and employee details
- `Calendar` - Holiday/working day information per organization
- `Submission` - Daily submission records with question_date and is_active status

## Error Handling
- Returns 401 if authentication token is invalid
- Returns 400 if required parameters are missing or invalid
- Returns 500 if server error occurs during calculation
- Skips employees with:
  - `is_active = false`
  - No incentive value (null or 0)
  - No submissions in the date range

## Sample cURL Request
```bash
curl -X POST http://localhost:8080/dashboardSummary/calculate-employee-incentive \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "orgId": 1,
    "fromDate": "2026-01-01",
    "toDate": "2026-01-31"
  }'
```

## Notes
- Results are sorted by incentive amount (descending)
- Incentive values are calculated to 2 decimal places
- Only active employees with incentive > 0 are included
- Working day definition: `is_active = false` in calendar table
