# Intelligent Alert Escalation & Resolution System

A sophisticated, production-ready alert management system for fleet operations that automatically manages, escalates, and resolves alerts from multiple source modules using configurable rules and intelligent automation.

---

## Core Strengths

### 1. **Intelligent Rule Engine**
- **DSL-Based Configuration**: Rules defined in JSON/YAML files, no code changes required
- **Dynamic Evaluation**: Rules evaluated at runtime, supporting complex escalation logic
- **Multi-Condition Support**: Time-based, count-based, and condition-based auto-closure
- **Hot Reload**: Rules can be updated without application restart

### 2. **Automatic Escalation & Resolution**
- **Pattern-Based Escalation**: Automatically escalates alerts based on frequency and time windows
- **Condition-Based Auto-Closure**: Automatically closes alerts when specific conditions are met (e.g., document renewal)
- **State Transition Enforcement**: Validates and enforces proper alert state transitions (OPEN → ESCALATED → AUTO_CLOSED/RESOLVED)
- **Idempotent Operations**: Safe to run multiple times without side effects

### 3. **Enterprise-Grade Architecture**
- **Modular Design**: Clean separation of concerns using OOP principles
- **Transaction Management**: Proper handling of concurrent operations with `REQUIRES_NEW` transactions
- **Caching Strategy**: Caffeine-based caching for optimal performance
- **Background Processing**: Scheduled jobs for auto-closure and data retention
- **Comprehensive Error Handling**: Global exception handler with standardized responses

### 4. **Security & Compliance**
- **JWT Authentication**: Secure token-based authentication
- **Role-Based Access**: Support for different user roles (admin, operator)
- **Audit Trail**: Complete history of all alert state transitions
- **Data Retention**: Configurable retention policies for compliance

### 5. **Observability & Monitoring**
- **Health Indicators**: Custom health checks for system components
- **Metrics Integration**: Prometheus-ready metrics for monitoring
- **Structured Logging**: Comprehensive logging with file rotation
- **API Documentation**: OpenAPI/Swagger documentation

---

## Quick Start

### Prerequisites
- **Java 17** or higher (Compatible with Mac M2 ARM64)
- **Maven 3.8+**
- **Git** (optional)

### Step 1: Clone and Build
```bash
# Navigate to project directory
cd Yagesh

# Build the project
mvn clean install
```

### Step 2: Run the Application
```bash
# Start the Spring Boot application
mvn spring-boot:run
```

The application will start on **http://localhost:8080**

### Step 3: Access the System

#### **Web Dashboard**
- **URL**: http://localhost:8080
- **Default Credentials**:
  - Username: `admin`
  - Password: `admin123`

#### **Swagger UI (API Documentation)**
- **URL**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs

**To authorize Swagger:**
1. Click the **"Authorize"** button (lock icon) in Swagger UI
2. Login via the `/api/v1/auth/login` endpoint to get a JWT token
3. Enter the token in the format: `Bearer <your-token>`
4. Click **"Authorize"** and **"Close"**

#### **H2 Database Console**
- **URL**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:file:./data/alertdb`
- **Username**: `sa`
- **Password**: (leave empty)

**Steps to access H2 Console:**
1. Navigate to http://localhost:8080/h2-console
2. Enter the JDBC URL: `jdbc:h2:file:./data/alertdb`
3. Username: `sa`
4. Password: (leave blank)
5. Click **"Connect"**

---

## 📋 Key Implementations

### 1. Alert State Management
```java
// State transitions are enforced
OPEN → ESCALATED → AUTO_CLOSED/RESOLVED
```

**Features:**
- Valid state transition validation
- Idempotent operations (safe to retry)
- Complete audit trail for all transitions

### 2. Rule Engine Implementation

**Rule Configuration** (`src/main/resources/rules.json`):
```json
{
  "rules": [
    {
      "alertType": "OVERSPEEDING",
      "escalateIfCount": 3,
      "windowMinutes": 60,
      "escalationSeverity": "CRITICAL",
      "autoCloseIfNoRepeat": true,
      "autoCloseWindowMinutes": 120
    },
    {
      "alertType": "COMPLIANCE_DOCUMENT_EXPIRY",
      "autoCloseIf": "DOCUMENT_RENEWED",
      "escalationSeverity": "WARNING"
    }
  ]
}
```

**Rule Types:**
- **Escalation Rules**: Trigger escalation based on alert count within time window
- **Auto-Close Rules**: Close alerts based on time or condition
- **Condition-Based**: Auto-close when specific metadata conditions are met

### 3. Automatic Escalation Logic

**How it works:**
1. Alert is created with status `OPEN`
2. Rule engine evaluates recent alerts of the same type for the same driver
3. If threshold is met (e.g., 3 overspeeding alerts in 60 minutes), escalation triggers
4. All related alerts are escalated to the configured severity
5. Status changes to `ESCALATED`
6. History entry is created for audit

**Example:**
- 3 overspeeding alerts within 60 minutes → All escalated to `CRITICAL`
- 2 negative feedback alerts within 24 hours → Escalated to `CRITICAL`

### 4. Auto-Closure Mechanisms

**Time-Based Auto-Close:**
- Alerts automatically close if no repeat occurs within the configured window
- Example: Overspeeding alert auto-closes after 2 hours if no new incidents

**Condition-Based Auto-Close:**
- Alerts close when specific conditions are met in metadata
- Example: Compliance alert auto-closes when `condition: DOCUMENT_RENEWED` is set

### 5. Background Jobs

**Auto-Close Scheduler:**
- Runs every 5 minutes (configurable)
- Evaluates alerts for auto-closure conditions
- Processes alerts in batches for efficiency

**Data Retention Scheduler:**
- Removes alerts older than configured retention period (default: 90 days)
- Maintains database performance

**Rule Reload Scheduler:**
- Periodically reloads rules from configuration file
- Enables rule updates without restart

### 6. RESTful API

**Key Endpoints:**
- `POST /api/v1/alerts` - Create alert
- `GET /api/v1/alerts/{id}` - Get alert details
- `PUT /api/v1/alerts/{id}/resolve` - Manually resolve alert
- `PUT /api/v1/alerts/{id}/escalate` - Manually escalate alert
- `PATCH /api/v1/alerts/{id}/condition` - Update alert condition (triggers auto-close evaluation)
- `GET /api/v1/dashboard` - Get dashboard statistics
- `POST /api/v1/auth/login` - Authenticate and get JWT token

### 7. Frontend Dashboard

**Features:**
- Real-time alert statistics
- Interactive dashboard with clickable stat cards
- Alert filtering by status
- Demo scenarios for testing
- Manual escalation and resolution
- Alert-type-specific resolution messages

---

## 🏗️ Architecture Highlights

### Design Patterns
- **Service Layer Pattern**: Business logic separated from controllers
- **Repository Pattern**: Data access abstraction
- **Strategy Pattern**: Rule engine with pluggable rules
- **Factory Pattern**: Alert history creation
- **Builder Pattern**: Entity construction

### Technology Stack
- **Backend**: Spring Boot 3.2.0, Java 17
- **Database**: H2 (development), PostgreSQL-ready (production)
- **Security**: Spring Security with JWT
- **Caching**: Caffeine
- **API Documentation**: OpenAPI 3 / Swagger
- **Monitoring**: Micrometer, Prometheus
- **Frontend**: Pure HTML, CSS, JavaScript (no framework dependencies)

### Key Components

**Services:**
- `AlertService`: Core alert CRUD operations
- `RuleEvaluationService`: Rule evaluation and escalation logic
- `DashboardService`: Aggregation and analytics
- `AuthService`: Authentication and authorization

**Repositories:**
- `AlertRepository`: Alert data access
- `AlertHistoryRepository`: Audit trail management
- `UserRepository`: User management

**Engines:**
- `RuleEngine`: Rule evaluation engine
- `RuleLoader`: Configuration file loader

---

## 📊 Demo Scenarios

The dashboard includes three built-in demo scenarios:

### 1. Overspeeding Escalation
- Creates 3 overspeeding alerts for the same driver
- Demonstrates automatic escalation to CRITICAL severity
- Shows rule-based escalation in action

### 2. Compliance Auto-Closure
- Creates a compliance document expiry alert
- Updates condition to `DOCUMENT_RENEWED`
- Demonstrates condition-based auto-closure

### 3. Negative Feedback Escalation
- Creates 2 negative feedback alerts
- Demonstrates escalation based on feedback patterns

---

## 🔧 Configuration

### Application Configuration (`application.yml`)

**Server:**
- Port: 8080
- Context Path: `/`

**Database:**
- H2 file-based database
- Location: `./data/alertdb`
- Auto-update schema

**Caching:**
- Type: Caffeine
- Max Size: 1000 entries
- TTL: 600 seconds

**Scheduling:**
- Auto-close: Every 5 minutes
- Rule reload: Every 5 minutes
- Data retention: 90 days

### Rule Configuration (`rules.json`)

Rules are defined in JSON format and can be updated without code changes:
- Alert type mapping
- Escalation thresholds
- Time windows
- Auto-close conditions

---

## 🔐 Security

### Authentication
- JWT-based authentication
- Token expiration: 24 hours
- Refresh token: 7 days

### Default Users
- **Admin**: `admin` / `admin123`
- **Operator**: `operator` / `operator123`

### Authorization
- Role-based access control
- API endpoints protected by JWT
- Swagger UI requires authentication

---

## 📈 Monitoring & Health Checks

### Health Endpoints
- **Health**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Prometheus**: http://localhost:8080/actuator/prometheus

### Custom Health Indicators
- Rule engine health check
- Database connectivity check

### Metrics
- Alert creation rate
- Escalation count
- Auto-close count
- Resolution count
- Job execution metrics

---

## 🧪 Testing

### Manual Testing via Dashboard
1. Access the web dashboard at http://localhost:8080
2. Use the demo scenarios to test escalation and auto-closure
3. Manually create alerts and test different scenarios

### API Testing via Swagger
1. Access Swagger UI at http://localhost:8080/swagger-ui.html
2. Authorize with JWT token
3. Test endpoints interactively

### Database Verification
1. Access H2 Console at http://localhost:8080/h2-console
2. Query `alerts` table to see alert data
3. Query `alert_history` table to see audit trail
4. Query `users` table to see user data

---

## 📝 API Usage Examples

### Create Alert
```bash
curl -X POST http://localhost:8080/api/v1/alerts \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "alertType": "OVERSPEEDING",
    "severity": "WARNING",
    "driverId": "DRIVER_001",
    "metadata": {
      "speed": 85,
      "limit": 60,
      "location": "Highway 101"
    }
  }'
```

### Get Dashboard Stats
```bash
curl -X GET http://localhost:8080/api/v1/dashboard \
  -H "Authorization: Bearer <token>"
```

### Resolve Alert
```bash
curl -X PUT http://localhost:8080/api/v1/alerts/{alertId}/resolve \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Issue resolved"
  }'
```

---

## 🐛 Troubleshooting

### Application Won't Start
- Check if port 8080 is available
- Verify Java 17 is installed: `java -version`
- Check Maven installation: `mvn -version`

### Database Connection Issues
- Ensure `./data/` directory exists
- Check H2 console URL format: `jdbc:h2:file:./data/alertdb`
- Verify username is `sa` and password is empty

### Swagger Authorization Issues
- Ensure you're using the correct token format: `Bearer <token>`
- Check token expiration (24 hours)
- Re-login if token expired

### Escalation Not Triggering
- Verify rules are loaded correctly (check logs)
- Ensure alert count meets threshold
- Check time window configuration
- Verify alerts are for the same driver and type

---

## 📚 Project Structure

```
Yagesh/
├── src/main/java/com/movesync/alert/
│   ├── config/          # Configuration classes
│   ├── controller/      # REST controllers
│   ├── domain/          # Domain models and enums
│   ├── dto/             # Data transfer objects
│   ├── engine/          # Rule engine implementation
│   ├── exception/       # Exception handlers
│   ├── monitoring/      # Health indicators and metrics
│   ├── repository/      # Data access layer
│   ├── scheduler/       # Background jobs
│   ├── security/       # Security configuration
│   └── service/        # Business logic
├── src/main/resources/
│   ├── application.yml  # Application configuration
│   ├── rules.json      # Rule definitions
│   └── static/         # Frontend files
└── pom.xml             # Maven dependencies
```

---

## 🎯 Production Readiness

### Current State
- ✅ Core functionality implemented
- ✅ Security and authentication
- ✅ Error handling and validation
- ✅ Monitoring and health checks
- ✅ Comprehensive logging
- ✅ API documentation

### Production Recommendations
- Switch to PostgreSQL database
- Configure production JWT secret
- Set up proper logging aggregation
- Configure production caching
- Set up monitoring dashboards
- Implement rate limiting
- Add request/response logging
- Configure CORS for production domains

---

## 📞 Support

For issues, questions, or contributions:
@yageshmishra118389@gmail.com

---

## 📄 License

This project is part of the Intelligent Alert Escalation & Resolution System.

---

**Built with ❤️ using Spring Boot, Java 17, and modern software engineering practices.**
