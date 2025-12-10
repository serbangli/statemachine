#!/bin/bash

# State Machine Database Schema Initialization Script
# This script initializes the schema in an existing database
# Use this if the database already exists and you just want to initialize/update the schema

# Configuration
DB_NAME="statemachine"
DB_USER="postgres"
DB_PASSWORD="postgres"
DB_HOST="localhost"
DB_PORT="5432"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}State Machine Database Schema Initialization${NC}"
echo "=============================================="

# Check if psql is available
if ! command -v psql &> /dev/null; then
    echo -e "${RED}Error: psql command not found. Please install PostgreSQL client tools.${NC}"
    exit 1
fi

# Set PGPASSWORD environment variable
export PGPASSWORD="$DB_PASSWORD"

# Check if database exists
DB_EXISTS=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -tAc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'")

if [ "$DB_EXISTS" != "1" ]; then
    echo -e "${RED}Error: Database '$DB_NAME' does not exist.${NC}"
    echo -e "${YELLOW}Please run create-database.sh first to create the database.${NC}"
    unset PGPASSWORD
    exit 1
fi

# Run initialization script
echo -e "${YELLOW}Initializing database schema...${NC}"
if [ -f "scripts/init-db.sql" ]; then
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "scripts/init-db.sql"
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Database schema initialized successfully!${NC}"
    else
        echo -e "${RED}Error: Failed to initialize database schema.${NC}"
        unset PGPASSWORD
        exit 1
    fi
else
    echo -e "${RED}Error: init-db.sql not found.${NC}"
    unset PGPASSWORD
    exit 1
fi

# Unset password
unset PGPASSWORD

echo -e "${GREEN}Schema initialization completed!${NC}"

