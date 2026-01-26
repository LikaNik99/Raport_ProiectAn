-- Blackjack Database Schema for PostgreSQL

-- Create ENUM type for game results
CREATE TYPE game_result AS ENUM ('win', 'lose', 'push');

-- Users table
CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    balance DECIMAL(10,2) DEFAULT 1000.00,
    points INT DEFAULT 0,
    total_games INT DEFAULT 0,
    wins INT DEFAULT 0,
    losses INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for users table
CREATE INDEX IF NOT EXISTS idx_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_points ON users(points DESC);

-- Game sessions table
CREATE TABLE IF NOT EXISTS game_sessions (
    session_id SERIAL PRIMARY KEY,
    room_id VARCHAR(50) NOT NULL,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP NULL,
    total_pot DECIMAL(10,2),
    winner_id INT,
    CONSTRAINT fk_winner FOREIGN KEY (winner_id) REFERENCES users(user_id)
);

-- Create indexes for game_sessions table
CREATE INDEX IF NOT EXISTS idx_room_id ON game_sessions(room_id);
CREATE INDEX IF NOT EXISTS idx_start_time ON game_sessions(start_time DESC);

-- Session players table
CREATE TABLE IF NOT EXISTS session_players (
    id SERIAL PRIMARY KEY,
    session_id INT NOT NULL,
    user_id INT NOT NULL,
    bet_amount DECIMAL(10,2),
    result game_result DEFAULT 'lose',
    winnings DECIMAL(10,2) DEFAULT 0.00,
    CONSTRAINT fk_session FOREIGN KEY (session_id) REFERENCES game_sessions(session_id) ON DELETE CASCADE,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Create indexes for session_players table
CREATE INDEX IF NOT EXISTS idx_session_id ON session_players(session_id);
CREATE INDEX IF NOT EXISTS idx_user_id ON session_players(user_id);
