# Database Initialization Scripts

This directory contains scripts to initialize the PostgreSQL database for the State Machine application.

## Prerequisites

- PostgreSQL server installed and running
- PostgreSQL client tools (`psql`) installed
- Access to PostgreSQL with superuser privileges (typically `postgres` user)

## Scripts

### 1. `create-database.sh`
Creates the database and initializes the schema. This is the main script to run for a fresh setup.

**Usage:**
```bash
chmod +x scripts/create-database.sh
./scripts/create-database.sh
```

**What it does:**
- Creates the `statemachine` database
- Runs the schema initialization script
- Sets up all tables, indexes, and triggers

### 2. `init-db-only.sh`
Initializes/updates the schema in an existing database. Use this if the database already exists.

**Usage:**
```bash
chmod +x scripts/init-db-only.sh
./scripts/init-db-only.sh
```

### 3. `init-db.sql`
SQL script that creates all tables, indexes, triggers, and functions. Can be run manually if needed.

**Usage:**
```bash
psql -U postgres -d statemachine -f scripts/init-db.sql
```

## Configuration

Before running the scripts, you may need to adjust the following variables in the shell scripts:

- `DB_NAME`: Database name (default: `statemachine`)
- `DB_USER`: PostgreSQL user (default: `postgres`)
- `DB_PASSWORD`: PostgreSQL password (default: `postgres`)
- `DB_HOST`: Database host (default: `localhost`)
- `DB_PORT`: Database port (default: `5432`)

## Manual Setup

If you prefer to set up the database manually:

### Step 1: Create the database
```bash
psql -U postgres -c "CREATE DATABASE statemachine;"
```

### Step 2: Initialize the schema
```bash
psql -U postgres -d statemachine -f scripts/init-db.sql
```

## Database Schema

The initialization script creates the following tables:

1. **machine_definitions** - Stores state machine definitions loaded from XML
2. **states** - Stores states (START, REGULAR, END) for each definition
3. **transitions** - Stores transitions between states with conditions
4. **machines** - Stores machine instances with current state
5. **machine_context** - Stores key-value context data for machines

## Notes

- The scripts use `DROP TABLE IF EXISTS` for clean initialization. Be careful if you have existing data!
- The application uses JPA with `spring.jpa.hibernate.ddl-auto=update`, so tables will be created/updated automatically on startup if they don't exist
- These scripts are provided for manual setup and for production environments where you want explicit control over the schema

## Troubleshooting

### Permission Denied
If you get permission errors, make sure:
- The scripts are executable: `chmod +x scripts/*.sh`
- You have PostgreSQL client tools installed
- You have the correct database credentials

### Database Already Exists
The `create-database.sh` script will prompt you if the database already exists. You can choose to recreate it or keep the existing one.

### Connection Issues
Make sure:
- PostgreSQL server is running
- The connection parameters (host, port, user, password) are correct
- Your firewall allows connections to PostgreSQL

## Alternative: Using Docker

If you're using Docker for PostgreSQL, you can also initialize the database using:

```bash
docker exec -i <postgres-container> psql -U postgres < scripts/init-db.sql
```

Or create the database first:
```bash
docker exec -i <postgres-container> psql -U postgres -c "CREATE DATABASE statemachine;"
docker exec -i <postgres-container> psql -U postgres -d statemachine < scripts/init-db.sql
```

