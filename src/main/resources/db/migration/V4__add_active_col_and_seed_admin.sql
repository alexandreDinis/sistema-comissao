-- Add active column, default to true for existing (if any), but false for logic
ALTER TABLE users ADD COLUMN active BOOLEAN NOT NULL DEFAULT FALSE;

-- Update existing users to be active (optional, typically migrations handle schema, data fix is separate, but for dev ease we set true)
-- UPDATE users SET active = TRUE; 

-- Seed Default Admin
-- Password is 'admin123' hashed with BCrypt
INSERT INTO users (email, password, role, active) 
SELECT 'admin@empresa.com', '$2a$10$slYQmyNdGzTn7ZLBXBChFOC9f6kFjAqPhccnP6DxlZz.Mm.OR.v.6', 'ADMIN', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@empresa.com'
);
