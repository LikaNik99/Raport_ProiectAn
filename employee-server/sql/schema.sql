-- PostgreSQL Schema for logare_db
-- This script runs automatically when the database is created by docker-compose
-- Database 'logare_db' is already created via POSTGRES_DB environment variable

-- Users table with team leader support
CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  phone VARCHAR(20),
  address VARCHAR(255),
  job VARCHAR(100),
  team_leader_id INTEGER,
  FOREIGN KEY (team_leader_id) REFERENCES users(id)
);

-- Job titles lookup table
CREATE TABLE job_titles (
  id SERIAL PRIMARY KEY,
  title VARCHAR(100) NOT NULL UNIQUE,
  description TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Presence tracking (legacy - keep for compatibility)
CREATE TABLE presence (
  id SERIAL PRIMARY KEY,
  user_id INTEGER NOT NULL,
  time_in TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Login/logout history for audit trail
CREATE TABLE login_history (
  id SERIAL PRIMARY KEY,
  user_id INTEGER NOT NULL,
  user_name VARCHAR(100) NOT NULL,
  user_role VARCHAR(20) NOT NULL,
  login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  logout_time TIMESTAMP,
  session_duration_seconds INTEGER,
  event_type VARCHAR(20) NOT NULL DEFAULT 'LOGIN',
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Leave requests
CREATE TABLE leave_requests (
  id SERIAL PRIMARY KEY,
  user_id INTEGER NOT NULL,
  date_from DATE,
  date_to DATE,
  reason TEXT,
  status VARCHAR(20) DEFAULT 'PENDING',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Work sessions tracking (start/end work for the day)
-- IMPORTANT: work_date is the primary date field, used for filtering and statistics
CREATE TABLE work_sessions (
  id SERIAL PRIMARY KEY,
  user_id INTEGER NOT NULL,
  work_date DATE NOT NULL,
  start_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  end_time TIMESTAMP,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  FOREIGN KEY (user_id) REFERENCES users(id),
  UNIQUE(user_id, work_date)
);

-- Work hours configuration (global settings)
CREATE TABLE work_hours_config (
  id SERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  start_hour INTEGER NOT NULL,
  start_minute INTEGER NOT NULL DEFAULT 0,
  end_hour INTEGER NOT NULL,
  end_minute INTEGER NOT NULL DEFAULT 0,
  description TEXT
);

-- Working days configuration (which days are work days)
CREATE TABLE working_days (
  id SERIAL PRIMARY KEY,
  day_of_week INTEGER NOT NULL UNIQUE,
  is_working_day BOOLEAN NOT NULL DEFAULT TRUE
);

-- Team assignments (team leader -> workers)
CREATE TABLE team_assignments (
  id SERIAL PRIMARY KEY,
  team_leader_id INTEGER NOT NULL,
  worker_id INTEGER NOT NULL,
  assigned_date DATE NOT NULL DEFAULT CURRENT_DATE,
  FOREIGN KEY (team_leader_id) REFERENCES users(id),
  FOREIGN KEY (worker_id) REFERENCES users(id),
  UNIQUE(team_leader_id, worker_id)
);

-- Salary Management System Tables

-- Hourly rates table with history tracking
CREATE TABLE hourly_rates (
  id SERIAL PRIMARY KEY,
  user_id INTEGER NOT NULL,
  hourly_rate DECIMAL(10,2) NOT NULL,
  valid_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  valid_to TIMESTAMP,
  created_by INTEGER,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (created_by) REFERENCES users(id),
  CONSTRAINT valid_rate CHECK (hourly_rate >= 0)
);

-- Indexes for hourly_rates
CREATE INDEX idx_hourly_rates_user_id ON hourly_rates(user_id);
CREATE INDEX idx_hourly_rates_valid_from ON hourly_rates(valid_from);

-- Monthly salary calculations table
CREATE TABLE salary_calculations (
  id SERIAL PRIMARY KEY,
  user_id INTEGER NOT NULL,
  month INTEGER NOT NULL CHECK (month >= 1 AND month <= 12),
  year INTEGER NOT NULL CHECK (year >= 2000),
  hours_worked DECIMAL(10,2) NOT NULL,
  hourly_rate DECIMAL(10,2) NOT NULL,
  base_salary DECIMAL(10,2) NOT NULL,
  bonuses_total DECIMAL(10,2) NOT NULL DEFAULT 0,
  gross_salary DECIMAL(10,2) NOT NULL,
  tax_amount DECIMAL(10,2) NOT NULL,
  net_salary DECIMAL(10,2) NOT NULL,
  is_published BOOLEAN NOT NULL DEFAULT FALSE,
  published_at TIMESTAMP,
  published_by INTEGER,
  calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (published_by) REFERENCES users(id),
  UNIQUE(user_id, month, year)
);

-- Indexes for salary_calculations
CREATE INDEX idx_salary_calculations_user_id ON salary_calculations(user_id);
CREATE INDEX idx_salary_calculations_month_year ON salary_calculations(month, year);
CREATE INDEX idx_salary_calculations_published ON salary_calculations(is_published);

-- Salary bonuses table
CREATE TABLE salary_bonuses (
  id SERIAL PRIMARY KEY,
  salary_calculation_id INTEGER NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  description VARCHAR(500) NOT NULL,
  added_by INTEGER,
  added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (salary_calculation_id) REFERENCES salary_calculations(id) ON DELETE CASCADE,
  FOREIGN KEY (added_by) REFERENCES users(id),
  CONSTRAINT positive_bonus CHECK (amount >= 0)
);

-- Index for salary_bonuses
CREATE INDEX idx_salary_bonuses_calculation_id ON salary_bonuses(salary_calculation_id);

-- Insert default admin user
-- Password is 'admin123' (IMPORTANT: Change in production!)
INSERT INTO users (id, name, role, password_hash) 
VALUES (1, 'Admin', 'ADMIN', 'admin123')
ON CONFLICT (id) DO NOTHING;

-- Insert default job titles
INSERT INTO job_titles (title, description) VALUES
('Lăcătuș', 'Lucrător specializat în prelucrarea metalelor'),
('Sudor', 'Specialist în sudură'),
('Electrician', 'Tehnician electrician'),
('Mecanic', 'Mecanic industrial'),
('Operator mașini', 'Operator mașini CNC'),
('Inginer', 'Inginer de producție'),
('Tehnician', 'Tehnician de întreținere')
ON CONFLICT (title) DO NOTHING;

-- Insert default work hours configuration (08:00 - 17:00)
INSERT INTO work_hours_config (id, name, start_hour, start_minute, end_hour, end_minute, description)
VALUES (1, 'Program Standard', 8, 0, 17, 0, 'Program de lucru standard: 08:00 - 17:00')
ON CONFLICT (id) DO NOTHING;

-- Insert default working days (Monday=1 to Friday=5 are working days)
INSERT INTO working_days (day_of_week, is_working_day) VALUES
(1, TRUE),  -- Monday
(2, TRUE),  -- Tuesday
(3, TRUE),  -- Wednesday
(4, TRUE),  -- Thursday
(5, TRUE),  -- Friday
(6, FALSE), -- Saturday
(7, FALSE)  -- Sunday
ON CONFLICT (day_of_week) DO NOTHING;

-- Create indexes for better performance
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_work_sessions_user_date ON work_sessions(user_id, work_date);
CREATE INDEX idx_work_sessions_date ON work_sessions(work_date);
CREATE INDEX idx_login_history_user ON login_history(user_id);
CREATE INDEX idx_leave_requests_user ON leave_requests(user_id);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);

