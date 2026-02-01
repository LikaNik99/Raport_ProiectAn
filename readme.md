# 🏋️ Fitness Tracker

A comprehensive full-stack web application for fitness tracking with user/admin authentication, workout logging, goal setting, and progress visualization.

![Fitness Tracker](https://img.shields.io/badge/Status-Ready-green) ![Node.js](https://img.shields.io/badge/Node.js-18+-green) ![License](https://img.shields.io/badge/License-MIT-blue)

## ✨ Features

### User Features
- 🔐 **Authentication** - Secure login/register with JWT
- 📊 **Dashboard** - Weekly summary with recommendations
- 💪 **Workout Logging** - Track cardio, strength, HIIT, stretching
- 🎯 **Goal Setting** - Create and track fitness objectives
- 📈 **Progress Tracking** - Weight and calorie charts
- 👤 **Profile Management** - Update personal information

### Admin Features
- 👥 **User Management** - View, block, delete users
- 📊 **Platform Statistics** - Global metrics and analytics

### Design
- 🌙 **Dark/Light Mode** - Theme toggle with persistence
- 📱 **Responsive Design** - Works on all devices
- ✨ **Modern UI** - Glassmorphism, gradients, animations

## 🚀 Quick Start

### Prerequisites
- [Node.js](https://nodejs.org/) v18 or higher
- npm (comes with Node.js)

### Installation

1. **Clone/Download the project**
   ```bash
   cd "Fitness Tracker"
   ```

2. **Install backend dependencies**
   ```bash
   cd backend
   npm install
   ```

3. **Start the server**
   ```bash
   npm start
   ```

4. **Open your browser**
   ```
   http://localhost:3000
   ```

### Default Admin Credentials
```
Email: admin@fitness.com
Password: admin123
```

## 📁 Project Structure

```
Fitness Tracker/
├── backend/
│   ├── config/          # Database configuration
│   ├── controllers/     # Business logic
│   ├── middleware/      # Auth & role middleware
│   ├── models/          # Sequelize models
│   ├── routes/          # API routes
│   ├── server.js        # Express server
│   └── package.json
├── frontend/
│   ├── css/
│   │   └── style.css    # Complete design system
│   ├── js/
│   │   └── app.js       # SPA application logic
│   └── index.html       # Main HTML file
├── docs/
│   ├── diagrams/        # UML diagrams
│   └── API.md           # API documentation
├── database.sqlite      # SQLite database (auto-created)
└── README.md
```

## 🔌 API Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/auth/register` | Create account | No |
| POST | `/api/auth/login` | Login | No |
| GET | `/api/auth/profile` | Get profile | Yes |
| PUT | `/api/auth/profile` | Update profile | Yes |
| GET | `/api/workouts` | List workouts | Yes |
| POST | `/api/workouts` | Add workout | Yes |
| PUT | `/api/workouts/:id` | Update workout | Yes |
| DELETE | `/api/workouts/:id` | Delete workout | Yes |
| GET | `/api/goals` | List goals | Yes |
| POST | `/api/goals` | Create goal | Yes |
| PUT | `/api/goals/:id` | Update goal | Yes |
| DELETE | `/api/goals/:id` | Delete goal | Yes |
| GET | `/api/progress` | Get history | Yes |
| POST | `/api/progress` | Record progress | Yes |
| GET | `/api/progress/dashboard` | Dashboard data | Yes |
| GET | `/api/admin/users` | List users | Admin |
| PUT | `/api/admin/users/:id/block` | Toggle block | Admin |
| DELETE | `/api/admin/users/:id` | Delete user | Admin |
| GET | `/api/admin/stats` | Platform stats | Admin |

## 🗄️ Database Schema

### Users
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| email | STRING | Unique email |
| password | STRING | Hashed password |
| name | STRING | Full name |
| age | INTEGER | User age |
| sex | ENUM | male/female/other |
| height | FLOAT | Height in cm |
| weight | FLOAT | Weight in kg |
| activityLevel | ENUM | beginner/intermediate/advanced |
| role | ENUM | user/admin |
| isBlocked | BOOLEAN | Account status |

### Workouts
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| userId | INTEGER | Foreign key |
| type | ENUM | cardio/strength/hiit/stretching/other |
| durationMinutes | INTEGER | Workout duration |
| caloriesBurned | INTEGER | Calories burned |
| workoutDate | DATE | Date of workout |
| notes | TEXT | Optional notes |

### Goals
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| userId | INTEGER | Foreign key |
| goalType | ENUM | Type of goal |
| targetValue | FLOAT | Target to achieve |
| targetUnit | STRING | Unit of measurement |
| currentValue | FLOAT | Current progress |
| deadline | DATE | Goal deadline |
| isCompleted | BOOLEAN | Completion status |

### Progress
| Column | Type | Description |
|--------|------|-------------|
| id | INTEGER | Primary key |
| userId | INTEGER | Foreign key |
| weight | FLOAT | Recorded weight |
| recordDate | DATE | Date of record |
| totalWorkouts | INTEGER | Weekly workouts |
| totalCalories | INTEGER | Weekly calories |

## 🛠️ Technologies

- **Backend**: Node.js, Express.js
- **Database**: SQLite, Sequelize ORM
- **Auth**: JWT, bcrypt
- **Frontend**: HTML5, CSS3, JavaScript
- **Charts**: Chart.js
- **Design**: CSS Custom Properties, Flexbox, Grid

## 📝 License

MIT License - feel free to use this project for learning or personal projects.

---

Made with ❤️ for fitness enthusiasts
