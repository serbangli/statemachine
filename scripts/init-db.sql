-- State Machine Database Initialization Script
-- This script creates the database and schema for the State Machine application

-- Create database (run this as postgres superuser)
-- Note: This command must be run separately as it cannot be executed within a transaction
-- psql -U postgres -c "CREATE DATABASE statemachine;"

-- Connect to the statemachine database
\c statemachine;

-- Create schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS public;

-- Set search path
SET search_path TO public;

-- Drop tables if they exist (for clean initialization)
DROP TABLE IF EXISTS machine_context CASCADE;
DROP TABLE IF EXISTS machines CASCADE;
DROP TABLE IF EXISTS transitions CASCADE;
DROP TABLE IF EXISTS states CASCADE;
DROP TABLE IF EXISTS machine_definitions CASCADE;

-- Create machine_definitions table
CREATE TABLE machine_definitions (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    xml_content TEXT NOT NULL
);

-- Create states table
CREATE TABLE states (
    id BIGSERIAL PRIMARY KEY,
    state_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('START', 'END', 'REGULAR')),
    machine_definition_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_states_machine_definition 
        FOREIGN KEY (machine_definition_id) 
        REFERENCES machine_definitions(id) 
        ON DELETE CASCADE
);

-- Create transitions table
CREATE TABLE transitions (
    id BIGSERIAL PRIMARY KEY,
    transition_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    source_state_id VARCHAR(255) NOT NULL,
    destination_state_id VARCHAR(255) NOT NULL,
    condition_expression TEXT,
    machine_definition_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_transitions_machine_definition 
        FOREIGN KEY (machine_definition_id) 
        REFERENCES machine_definitions(id) 
        ON DELETE CASCADE
);

-- Create machines table
CREATE TABLE machines (
    id BIGSERIAL PRIMARY KEY,
    machine_definition_id VARCHAR(255) NOT NULL,
    current_state_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_machines_machine_definition 
        FOREIGN KEY (machine_definition_id) 
        REFERENCES machine_definitions(id)
);

-- Create machine_context table (for storing key-value pairs)
CREATE TABLE machine_context (
    machine_id BIGINT NOT NULL,
    context_key VARCHAR(255) NOT NULL,
    context_value TEXT,
    PRIMARY KEY (machine_id, context_key),
    CONSTRAINT fk_machine_context_machine 
        FOREIGN KEY (machine_id) 
        REFERENCES machines(id) 
        ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_states_machine_definition_id ON states(machine_definition_id);
CREATE INDEX idx_transitions_machine_definition_id ON transitions(machine_definition_id);
CREATE INDEX idx_transitions_source_state_id ON transitions(source_state_id);
CREATE INDEX idx_machines_machine_definition_id ON machines(machine_definition_id);
CREATE INDEX idx_machines_current_state_id ON machines(current_state_id);
CREATE INDEX idx_machine_context_machine_id ON machine_context(machine_id);

-- Create function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at on machines table
CREATE TRIGGER update_machines_updated_at 
    BEFORE UPDATE ON machines 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE machine_definitions IS 'Stores state machine definitions loaded from XML files';
COMMENT ON TABLE states IS 'Stores states (start, regular, end) for each machine definition';
COMMENT ON TABLE transitions IS 'Stores transitions between states with optional conditions';
COMMENT ON TABLE machines IS 'Stores instances of state machines with their current state';
COMMENT ON TABLE machine_context IS 'Stores key-value context data for each machine instance';

COMMENT ON COLUMN states.type IS 'Type of state: START, END, or REGULAR';
COMMENT ON COLUMN transitions.condition_expression IS 'SpEL expression that must evaluate to true for transition to be available';
COMMENT ON COLUMN machines.current_state_id IS 'Current state ID of the machine instance';

-- Grant permissions (adjust as needed for your setup)
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO postgres;

-- Success message
DO $$
BEGIN
    RAISE NOTICE 'Database initialization completed successfully!';
END $$;

