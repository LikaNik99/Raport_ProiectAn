-- Migration: Add job column to users table
-- Run this on existing databases that don't have the job column

ALTER TABLE users ADD COLUMN IF NOT EXISTS job VARCHAR(100);

-- Optional: Update existing users with default job value
UPDATE users SET job = 'Angajat' WHERE job IS NULL;

COMMENT ON COLUMN users.job IS 'Job title or position of the employee';
