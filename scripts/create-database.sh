#!/bin/bash

# State Machine Database Creation Script
# This script creates the PostgreSQL database for the State Machine application

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

echo -e "${YELLOW}State Machine Database Setup${NC}"
echo "================================"

# Check if psql is available
if ! command -v psql &> /dev/null; then
    echo -e "${RED}Error: psql command not found. Please install PostgreSQL client tools.${NC}"
    exit 1
fi

# Set PGPASSWORD environment variable
export PGPASSWORD="$DB_PASSWORD"

# Check if database already exists
DB_EXISTS=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -tAc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'")

if [ "$DB_EXISTS" = "1" ]; then
    echo -e "${YELLOW}Database '$DB_NAME' already exists.${NC}"
    read -p "Do you want to drop and recreate it? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${YELLOW}Dropping existing database...${NC}"
        psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -c "DROP DATABASE IF EXISTS $DB_NAME;"
    else
        echo -e "${GREEN}Keeping existing database.${NC}"
        exit 0
    fi
fi

# Create database
echo -e "${YELLOW}Creating database '$DB_NAME'...${NC}"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -c "CREATE DATABASE $DB_NAME;" 2>&1

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Database '$DB_NAME' created successfully!${NC}"
else
    echo -e "${RED}Error: Failed to create database.${NC}"
    unset PGPASSWORD
    exit 1
fi

# Run initialization script
echo -e "${YELLOW}Running initialization script...${NC}"
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
    echo -e "${YELLOW}Warning: init-db.sql not found. Skipping schema initialization.${NC}"
fi

# Unset password
unset PGPASSWORD

echo -e "${GREEN}Database setup completed!${NC}"
echo ""
echo "You can now start the Spring Boot application."
echo "Database connection details:"
echo "  Host: $DB_HOST"
echo "  Port: $DB_PORT"
echo "  Database: $DB_NAME"
echo "  User: $DB_USER"

