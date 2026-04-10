-- Initialize Hospital Database
-- This script runs when PostgreSQL container starts

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Set timezone
SET timezone = 'UTC';

-- Create indexes for better performance
-- These will be created by JPA/Hibernate but we can add them here for optimization

-- Additional database optimizations
-- These settings can improve performance for high-concurrency scenarios

-- Log statement for debugging
DO $$
BEGIN
    RAISE NOTICE 'Hospital database initialized successfully';
END $$;
