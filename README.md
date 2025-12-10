# State Machine Application

A Spring Boot application for managing state machines with XML-based definitions, condition-based transitions, and PostgreSQL persistence.

## Features

- Define state machines in XML format
- Condition-based transitions using Spring Expression Language (SpEL)
- Persistent state machine instances in PostgreSQL
- RESTful API for managing state machines
- Full test coverage

## Running Tests

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=StateMachineIntegrationTest
```

### Run Tests with Coverage
```bash
mvn test jacoco:report
```

## Test Structure

### Integration Tests
- `StateMachineIntegrationTest` - Full end-to-end tests of state machine functionality
  - Loading machine definitions from XML
  - Creating machine instances
  - Executing transitions with and without conditions
  - Context management
  - Complete state machine flow

### Unit Tests
- `XmlMachineParserTest` - Tests XML parsing functionality
- `ConditionEvaluatorTest` - Tests condition evaluation using SpEL
- `MachineControllerTest` - Tests REST API endpoints

### Test Configuration
Tests use H2 in-memory database (configured in `application-test.properties`) so no PostgreSQL setup is required for testing.

## Example Test Flow

The integration tests demonstrate a complete state machine flow:

1. Load machine definition from XML (`first.xml`)
2. Create a machine instance with initial context (`name: "john"`)
3. Execute transition `t0` (start → one)
4. Execute transition `t1` (one → two) - condition: `name == 'john'`
5. Execute transition `t2` (two → end)

## Database Setup

### Option 1: Using Docker (Recommended for Development)

1. Start PostgreSQL using Docker Compose:
```bash
docker-compose up -d
```

The database will be automatically initialized with the schema.

### Option 2: Manual PostgreSQL Setup

1. Create the database:
```bash
./scripts/create-database.sh
```

Or manually:
```bash
psql -U postgres -c "CREATE DATABASE statemachine;"
psql -U postgres -d statemachine -f scripts/init-db.sql
```

### Option 3: Using Docker Container (Existing Container)

If you have a PostgreSQL container running:
```bash
./scripts/docker-init.sh
```

For more details, see [scripts/README.md](scripts/README.md)

## Building and Running

### Build the Application
```bash
mvn clean package
```

### Run the Application
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

**Note:** Make sure PostgreSQL is running and the database is initialized before starting the application.

## Web UI

The application includes a modern web-based UI for managing state machines. After starting the application, navigate to:

```
http://localhost:8080
```

### UI Features

1. **Machine Definitions**
   - Load machine definitions from XML files
   - View all available definitions
   - Inspect definition details (states, transitions)

2. **Create Machine Instances**
   - Create new machine instances from definitions
   - Set initial context (key-value pairs)

3. **Manage Machine Instances**
   - View machine state and context
   - See available transitions from current state
   - Execute transitions
   - Update machine context

4. **Real-time Updates**
   - Automatic refresh of available transitions
   - Visual feedback for all operations
   - Error handling with user-friendly messages

## API Endpoints

- `POST /api/machine-definitions/load-from-file?fileName=first.xml` - Load machine definition
- `GET /api/machine-definitions` - Get all definitions
- `GET /api/machine-definitions/{id}` - Get specific definition
- `POST /api/machines` - Create machine instance
- `GET /api/machines/{id}` - Get machine instance
- `GET /api/machines/definition/{definitionId}` - Get all machines for a definition
- `POST /api/machines/{id}/transitions/execute` - Execute transition
- `GET /api/machines/{id}/transitions/available` - Get available transitions
- `PUT /api/machines/{id}/context` - Update machine context

