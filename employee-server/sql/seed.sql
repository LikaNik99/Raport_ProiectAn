-- PostgreSQL seed data for logare_db
-- Additional test users (admin user already created in schema.sql)

-- Add some test users for development
INSERT INTO users (id, name, role, password_hash) VALUES
 (2001, 'HR Manager', 'HR', 'admin123'),
 (1001, 'Angajat Test', 'WORKER', 'parola1')
ON CONFLICT (id) DO NOTHING;

-- Fix the users ID sequence to start after the highest existing ID
-- This ensures new users (from Excel import or manual creation) get correct auto-incremented IDs
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));

-- Note: work_hours_config and working_days are already configured in schema.sql
-- No need to duplicate them here

