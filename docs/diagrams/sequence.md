# Sequence Diagrams

## 1. User Registration Flow

```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend
    participant S as Server
    participant DB as Database
    
    U->>F: Fill registration form
    F->>F: Validate input
    F->>S: POST /api/auth/register
    S->>S: Validate request body
    S->>DB: Check if email exists
    DB-->>S: Email status
    
    alt Email already exists
        S-->>F: 400 Email already registered
        F-->>U: Show error message
    else Email available
        S->>S: Hash password (bcrypt)
        S->>DB: Create user record
        DB-->>S: User created
        S->>S: Generate JWT token
        S-->>F: 201 {user, token}
        F->>F: Store token in localStorage
        F-->>U: Redirect to dashboard
    end
```

## 2. User Login Flow

```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend
    participant S as Server
    participant DB as Database
    
    U->>F: Enter email & password
    F->>S: POST /api/auth/login
    S->>DB: Find user by email
    DB-->>S: User record
    
    alt User not found
        S-->>F: 401 Invalid credentials
        F-->>U: Show error
    else User found
        S->>S: Compare password (bcrypt)
        alt Password invalid
            S-->>F: 401 Invalid credentials
            F-->>U: Show error
        else Password valid
            alt User is blocked
                S-->>F: 403 Account blocked
                F-->>U: Show blocked message
            else User active
                S->>S: Generate JWT token
                S-->>F: 200 {user, token}
                F->>F: Store token
                F-->>U: Redirect to dashboard
            end
        end
    end
```

## 3. Add Workout Flow

```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend
    participant S as Server
    participant MW as Auth Middleware
    participant DB as Database
    
    U->>F: Click "Add Workout"
    F-->>U: Show workout modal
    U->>F: Fill form & submit
    F->>S: POST /api/workouts (with JWT)
    S->>MW: Verify JWT token
    
    alt Token invalid/expired
        MW-->>F: 401 Unauthorized
        F-->>U: Redirect to login
    else Token valid
        MW->>MW: Attach user to request
        MW->>S: Continue request
        S->>S: Validate workout data
        S->>DB: Insert workout
        DB-->>S: Workout record
        S-->>F: 201 Workout created
        F->>F: Close modal
        F->>F: Refresh workout list
        F-->>U: Show success toast
    end
```

## 4. Goal Progress Update Flow

```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend
    participant S as Server
    participant DB as Database
    
    U->>F: Click "Update Progress"
    F-->>U: Prompt for new value
    U->>F: Enter new progress value
    F->>S: PUT /api/goals/:id/progress
    S->>DB: Find goal by ID
    DB-->>S: Goal record
    
    alt Goal not found
        S-->>F: 404 Goal not found
        F-->>U: Show error
    else Goal found
        S->>S: Update currentValue
        S->>S: Check if currentValue >= targetValue
        alt Goal completed
            S->>DB: Set isCompleted = true
        end
        S->>DB: Save goal
        DB-->>S: Updated goal
        S-->>F: 200 {goal, message}
        F->>F: Refresh goals list
        F-->>U: Show completion toast
    end
```

## 5. Dashboard Load Flow

```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend
    participant S as Server
    participant DB as Database
    
    U->>F: Navigate to Dashboard
    F->>S: GET /api/progress/dashboard
    S->>DB: Get this week's workouts
    DB-->>S: Workout records
    S->>DB: Get latest progress
    DB-->>S: Progress record
    S->>DB: Get recent 5 workouts
    DB-->>S: Workout list
    S->>S: Calculate stats
    S->>S: Generate recommendations
    S-->>F: 200 {weekSummary, currentWeight, recentWorkouts, recommendations}
    F->>F: Update stat cards
    F->>F: Render workout list
    F->>F: Render recommendations
    F-->>U: Display dashboard
```

## 6. Admin Block User Flow

```mermaid
sequenceDiagram
    participant A as Admin
    participant F as Frontend
    participant S as Server
    participant MW as Role Middleware
    participant DB as Database
    
    A->>F: Click "Block User"
    F->>S: PUT /api/admin/users/:id/block
    S->>MW: Check user role
    
    alt Not admin
        MW-->>F: 403 Access denied
        F-->>A: Show permission error
    else Is admin
        MW->>S: Continue request
        S->>DB: Find user by ID
        DB-->>S: User record
        
        alt Target is admin
            S-->>F: 400 Cannot block admin
            F-->>A: Show error
        else Target is regular user
            S->>S: Toggle isBlocked
            S->>DB: Update user
            DB-->>S: Updated user
            S-->>F: 200 User blocked/unblocked
            F->>F: Refresh user table
            F-->>A: Show success message
        end
    end
```

## 7. Progress Chart Update Flow

```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend
    participant S as Server
    participant DB as Database
    participant C as Chart.js
    
    U->>F: Select period (week/month/year)
    F->>S: GET /api/progress/stats?period=month
    S->>S: Calculate date range
    S->>DB: Get workouts in range
    DB-->>S: Workouts
    S->>DB: Get progress in range
    DB-->>S: Progress records
    S->>S: Aggregate statistics
    S->>S: Format for charts
    S-->>F: 200 {workouts, weight, calories}
    F->>C: Destroy old charts
    F->>C: Create weight line chart
    F->>C: Create calories bar chart
    C-->>F: Charts rendered
    F-->>U: Display updated charts
```
