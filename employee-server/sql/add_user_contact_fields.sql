-- Add phone and address columns to users table
-- Run this to update existing database

ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS address VARCHAR(255);
