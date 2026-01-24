-- Fix the users table ID sequence to match existing data
-- This ensures that the next auto-generated ID will be correct

-- Get the maximum ID from the users table and set the sequence to start from there
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 0) FROM users) + 1, false);

-- Verify the sequence is set correctly
-- SELECT currval('users_id_seq');
