# Entity Relationship Diagram (ERD)

## Fitness Tracker Database Schema

```mermaid
erDiagram
    USERS {
        int id PK "Primary Key"
        string email UK "Unique, Required"
        string password "Hashed, Required"
        string name "Required"
        int age "Optional"
        enum sex "male/female/other"
        float height "cm, Optional"
        float weight "kg, Optional"
        enum activity_level "beginner/intermediate/advanced"
        enum role "user/admin"
        boolean is_blocked "Default: false"
        datetime created_at "Auto"
        datetime updated_at "Auto"
    }
    
    WORKOUTS {
        int id PK "Primary Key"
        int user_id FK "References Users"
        enum type "cardio/strength/hiit/stretching/other"
        int duration_minutes "Required, Min: 1"
        int calories_burned "Optional"
        date workout_date "Required"
        text notes "Optional"
        datetime created_at "Auto"
        datetime updated_at "Auto"
    }
    
    GOALS {
        int id PK "Primary Key"
        int user_id FK "References Users"
        enum goal_type "weight_loss/muscle_gain/endurance/workouts_per_week/steps/custom"
        float target_value "Required"
        string target_unit "kg/minutes/steps/workouts"
        float current_value "Default: 0"
        date deadline "Optional"
        boolean is_completed "Default: false"
        text description "Optional"
        datetime created_at "Auto"
        datetime updated_at "Auto"
    }
    
    PROGRESS {
        int id PK "Primary Key"
        int user_id FK "References Users"
        float weight "kg, Optional"
        date record_date "Required"
        int total_workouts "Default: 0"
        int total_calories "Default: 0"
        int total_duration "minutes, Default: 0"
        text notes "Optional"
        datetime created_at "Auto"
        datetime updated_at "Auto"
    }
    
    USERS ||--o{ WORKOUTS : "logs"
    USERS ||--o{ GOALS : "sets"
    USERS ||--o{ PROGRESS : "tracks"
```

## Table Relationships

### One-to-Many Relationships

1. **Users → Workouts**
   - One user can have many workout entries
   - Each workout belongs to exactly one user
   - Cascade delete: deleting user removes all workouts

2. **Users → Goals**
   - One user can set multiple fitness goals
   - Each goal belongs to exactly one user
   - Cascade delete: deleting user removes all goals

3. **Users → Progress**
   - One user can have multiple progress records
   - Each progress entry belongs to exactly one user
   - Cascade delete: deleting user removes all progress

## Data Types & Constraints

### ENUM Values

| Field | Values |
|-------|--------|
| sex | male, female, other |
| activity_level | beginner, intermediate, advanced |
| role | user, admin |
| workout.type | cardio, strength, hiit, stretching, other |
| goal.goal_type | weight_loss, muscle_gain, endurance, workouts_per_week, steps, custom |

### Constraints

- `email`: Unique, valid email format
- `password`: Minimum 6 characters, stored as bcrypt hash
- `duration_minutes`: Minimum 1
- `calories_burned`: Minimum 0
- `height`: Between 100-250 cm
- `weight`: Between 30-300 kg
- `age`: Between 10-120
