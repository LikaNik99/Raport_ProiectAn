# Fitness Tracker API Documentation

## Base URL
```
http://localhost:3000/api
```

## Authentication
All protected endpoints require a JWT token in the Authorization header:
```
Authorization: Bearer <token>
```

---

## Auth Endpoints

### POST /auth/register
Create a new user account.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "John Doe",
  "age": 25,
  "sex": "male",
  "height": 175,
  "weight": 70,
  "activityLevel": "intermediate"
}
```

**Response (201):**
```json
{
  "success": true,
  "message": "Registration successful.",
  "data": {
    "user": { "id": 1, "email": "user@example.com", "name": "John Doe", ... },
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

---

### POST /auth/login
Authenticate user and receive token.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Login successful.",
  "data": {
    "user": { ... },
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

---

### GET /auth/profile
Get current user profile. **Requires Auth**

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "John Doe",
    "age": 25,
    "sex": "male",
    "height": 175,
    "weight": 70,
    "activityLevel": "intermediate",
    "role": "user"
  }
}
```

---

### PUT /auth/profile
Update user profile. **Requires Auth**

**Request Body:**
```json
{
  "name": "John Doe",
  "age": 26,
  "weight": 72
}
```

---

## Workout Endpoints

### GET /workouts
List user's workouts. **Requires Auth**

**Query Parameters:**
- `type` - Filter by type (cardio, strength, hiit, stretching, other)
- `startDate` - Filter from date (YYYY-MM-DD)
- `endDate` - Filter to date (YYYY-MM-DD)
- `limit` - Max results (default: 50)
- `offset` - Pagination offset

**Response (200):**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "type": "cardio",
      "durationMinutes": 30,
      "caloriesBurned": 300,
      "workoutDate": "2024-01-15",
      "notes": "Morning run"
    }
  ],
  "pagination": { "total": 1, "limit": 50, "offset": 0 }
}
```

---

### POST /workouts
Log a new workout. **Requires Auth**

**Request Body:**
```json
{
  "type": "cardio",
  "durationMinutes": 30,
  "caloriesBurned": 300,
  "workoutDate": "2024-01-15",
  "notes": "Morning run"
}
```

---

### PUT /workouts/:id
Update a workout. **Requires Auth**

---

### DELETE /workouts/:id
Delete a workout. **Requires Auth**

---

### GET /workouts/stats
Get workout statistics. **Requires Auth**

**Response (200):**
```json
{
  "success": true,
  "data": {
    "totalWorkouts": 10,
    "totalDuration": 300,
    "totalCalories": 3000,
    "byType": {
      "cardio": { "count": 5, "duration": 150, "calories": 1500 }
    }
  }
}
```

---

## Goal Endpoints

### GET /goals
List user's goals. **Requires Auth**

**Query Parameters:**
- `isCompleted` - Filter by completion (true/false)

---

### POST /goals
Create a new goal. **Requires Auth**

**Request Body:**
```json
{
  "goalType": "weight_loss",
  "targetValue": 5,
  "targetUnit": "kg",
  "currentValue": 0,
  "deadline": "2024-06-01",
  "description": "Lose 5kg by summer"
}
```

---

### PUT /goals/:id/progress
Update goal progress. **Requires Auth**

**Request Body:**
```json
{
  "currentValue": 2.5
}
```

---

## Progress Endpoints

### GET /progress
Get progress history. **Requires Auth**

---

### POST /progress
Record progress entry. **Requires Auth**

**Request Body:**
```json
{
  "weight": 72.5,
  "recordDate": "2024-01-15",
  "notes": "Feeling great!"
}
```

---

### GET /progress/stats
Get aggregated statistics. **Requires Auth**

**Query Parameters:**
- `period` - Time period (week, month, year)

---

### GET /progress/dashboard
Get dashboard data. **Requires Auth**

**Response (200):**
```json
{
  "success": true,
  "data": {
    "weekSummary": {
      "workouts": 3,
      "duration": 90,
      "calories": 900
    },
    "currentWeight": 72,
    "recentWorkouts": [...],
    "recommendations": [
      { "type": "motivation", "message": "Great job!", "icon": "🌟" }
    ]
  }
}
```

---

## Admin Endpoints

### GET /admin/users
List all users. **Requires Admin**

**Query Parameters:**
- `search` - Search by name/email
- `role` - Filter by role
- `isBlocked` - Filter by status

---

### PUT /admin/users/:id/block
Toggle user block status. **Requires Admin**

---

### DELETE /admin/users/:id
Delete user account. **Requires Admin**

---

### GET /admin/stats
Get platform statistics. **Requires Admin**

**Response (200):**
```json
{
  "success": true,
  "data": {
    "users": { "total": 100, "active": 95, "blocked": 5 },
    "workouts": { "total": 500, "thisWeek": 50 },
    "goals": { "total": 200, "completed": 80, "completionRate": 40 }
  }
}
```

---

## Error Responses

### 400 Bad Request
```json
{
  "success": false,
  "message": "Validation error message"
}
```

### 401 Unauthorized
```json
{
  "success": false,
  "message": "Access denied. No token provided."
}
```

### 403 Forbidden
```json
{
  "success": false,
  "message": "Access denied. Insufficient permissions."
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "Resource not found."
}
```

### 500 Server Error
```json
{
  "success": false,
  "message": "Internal server error"
}
```
