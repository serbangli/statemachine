# Database Initialization Scripts

This directory contains scripts to initialize and manage the PostgreSQL database for the State Machine application.

## Prerequisites

- PostgreSQL server installed and running (or a PostgreSQL Docker container)
- PostgreSQL client tools (`psql`) installed
- Access to PostgreSQL with superuser privileges (typically `postgres` user)
- Bash shell available to run the `.sh` scripts

## Scripts

### 1. `create-database.sh`
Creates the database and initializes the schema. This is the main script to run for a fresh local setup.

**Usage:**
```bash
chmod +x scripts/create-database.sh
./scripts/create-database.sh
```

**What it does:**
- Checks if the `statemachine` database exists and optionally drops it
- Creates the `statemachine` database
- Runs the schema initialization script (`init-db.sql`)
- Prints connection details when finished

### 2. `init-db-only.sh`
Initializes/updates the schema in an existing database. Use this if the database already exists and you just want to (re)apply the schema.

**Usage:**
```bash
chmod +x scripts/init-db-only.sh
./scripts/init-db-only.sh
```

**What it does:**
- Verifies that the `statemachine` database exists
- Runs `init-db.sql` against the existing database

### 3. `docker-init.sh`
Initializes the database schema inside a PostgreSQL Docker container.

**Defaults:**
- Container name: `statemachine-postgres`
- Database: `statemachine`
- User: `postgres`

**Usage:**
```bash
chmod +x scripts/docker-init.sh
./scripts/docker-init.sh
```

**What it does:**
- Verifies that the `statemachine-postgres` container exists
- Starts the container if it is stopped (via `docker-compose up -d postgres`)
- Runs `scripts/init-db.sql` inside the container

### 4. `init-db.sql`
SQL script that creates all tables, indexes, triggers, and functions. Can be run manually if needed.

**Usage:**
```bash
psql -U postgres -d statemachine -f scripts/init-db.sql
```

### 5. `clean.sql`
Utility script to truncate all tables in the `public` schema and reset identity sequences. Useful for resetting the database during development or tests.

**Usage:**
```bash
psql -U postgres -d statemachine -f scripts/clean.sql
```

## Configuration

Before running the shell scripts, you may need to adjust the following variables in `create-database.sh` and `init-db-only.sh`:

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

### Step 3: (Optional) Clean the database
```bash
psql -U postgres -d statemachine -f scripts/clean.sql
```

## Database Schema

The initialization script creates the following tables:

1. **machine_definitions** - Stores state machine definitions loaded from XML
2. **states** - Stores states (START, REGULAR, END) for each definition
3. **transitions** - Stores transitions between states with conditions
4. **machines** - Stores machine instances with current state
5. **machine_context** - Stores key-value context data for machines

It also:
- Creates indexes for common lookup patterns
- Adds a trigger to keep `machines.updated_at` in sync
- Adds comments to document the schema

## Notes

- The initialization script uses `DROP TABLE IF EXISTS` for clean initialization. Be careful if you have existing data.
- The application uses JPA with `spring.jpa.hibernate.ddl-auto=update`, so tables will be created/updated automatically on startup if they don't exist.
- These scripts are provided for manual setup and for environments where you want explicit control over the schema.

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
- PostgreSQL server (or Docker container) is running
- The connection parameters (host, port, user, password) are correct
- Your firewall allows connections to PostgreSQL

## Alternative: Using Docker Directly

If you're using Docker for PostgreSQL, you can also initialize the database using:

```bash
docker exec -i <postgres-container> psql -U postgres < scripts/init-db.sql
```

Or create the database first and then apply the schema:

```bash
docker exec -i <postgres-container> psql -U postgres -c "CREATE DATABASE statemachine;"
docker exec -i <postgres-container> psql -U postgres -d statemachine < scripts/init-db.sql
```
