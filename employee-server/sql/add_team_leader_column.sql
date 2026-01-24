-- Migration: Add team_leader_id column to users table
-- This column links a WORKER to their TEAM LEADER

ALTER TABLE users ADD COLUMN IF NOT EXISTS team_leader_id INTEGER;

-- Add foreign key constraint to reference team leaders
ALTER TABLE users 
  ADD CONSTRAINT users_team_leader_id_fkey 
  FOREIGN KEY (team_leader_id) 
  REFERENCES users(id) 
  ON DELETE SET NULL;

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_team_leader_id ON users(team_leader_id);

COMMENT ON COLUMN users.team_leader_id IS 'ID of the team leader for this worker (NULL for non-workers)';
