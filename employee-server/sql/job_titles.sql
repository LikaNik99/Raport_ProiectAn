-- Table for managing job titles/positions
CREATE TABLE IF NOT EXISTS job_titles (
  id SERIAL PRIMARY KEY,
  title VARCHAR(100) NOT NULL UNIQUE,
  description TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert some default job titles
INSERT INTO job_titles (title, description) VALUES
  ('Sudor', 'Specialist în sudură'),
  ('Electrician', 'Specialist în instalații electrice'),
  ('Lacatus', 'Specialist în prelucrare mecanică'),
  ('Tamplar', 'Specialist în prelucrare lemn'),
  ('Zugrav', 'Specialist în vopsitorie'),
  ('Team Leader', 'Șef de echipă'),
  ('Angajat', 'Poziție generală')
ON CONFLICT (title) DO NOTHING;

COMMENT ON TABLE job_titles IS 'Available job titles/positions for employees';
