#!/bin/bash

# State Machine Database Initialization using Docker
# This script initializes the database in a Docker container

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}State Machine Database Initialization (Docker)${NC}"
echo "=============================================="

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null && ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker not found. Please install Docker.${NC}"
    exit 1
fi

# Check if container exists
CONTAINER_NAME="statemachine-postgres"
CONTAINER_EXISTS=$(docker ps -a --filter "name=$CONTAINER_NAME" --format "{{.Names}}")

if [ -z "$CONTAINER_EXISTS" ]; then
    echo -e "${YELLOW}PostgreSQL container not found.${NC}"
    echo -e "${YELLOW}Please start the database using: docker-compose up -d${NC}"
    exit 1
fi

# Check if container is running
CONTAINER_RUNNING=$(docker ps --filter "name=$CONTAINER_NAME" --format "{{.Names}}")

if [ -z "$CONTAINER_RUNNING" ]; then
    echo -e "${YELLOW}Starting PostgreSQL container...${NC}"
    docker-compose up -d postgres
    echo -e "${YELLOW}Waiting for database to be ready...${NC}"
    sleep 5
fi

# Run initialization script
echo -e "${YELLOW}Initializing database schema...${NC}"
if [ -f "scripts/init-db.sql" ]; then
    docker exec -i "$CONTAINER_NAME" psql -U postgres -d statemachine < scripts/init-db.sql
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Database schema initialized successfully!${NC}"
    else
        echo -e "${RED}Error: Failed to initialize database schema.${NC}"
        exit 1
    fi
else
    echo -e "${RED}Error: init-db.sql not found.${NC}"
    exit 1
fi

echo -e "${GREEN}Database initialization completed!${NC}"
echo ""
echo "Database connection details:"
echo "  Host: localhost"
echo "  Port: 5432"
echo "  Database: statemachine"
echo "  User: postgres"
echo "  Password: postgres"

