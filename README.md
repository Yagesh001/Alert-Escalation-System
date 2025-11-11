# Intelligent Alert Escalation & Resolution System

A sophisticated fleet monitoring system that automatically manages, escalates, and resolves alerts from multiple source modules (Safety, Compliance, Feedback) using configurable rules and intelligent automation.

## 🚀 Features

### Core Functionality
- **Centralized Alert Management**: Unified API for ingesting alerts from multiple sources
- **Intelligent Rule Engine**: DSL-based configurable rules for escalation and auto-closure
- **Automatic Escalation**: Dynamic escalation based on alert patterns and thresholds
- **Auto-Closure**: Background job automatically closes alerts when conditions are met
- **Real-time Dashboard**: Comprehensive analytics and visualization
- **Audit Trail**: Complete history of alert state transitions

### Technical Highlights
- **RESTful API** with OpenAPI/Swagger documentation
- **JWT Authentication** for secure access
- **Caching Strategy** using Caffeine for optimal performance
- **Background Schedulers** for automated processing
- **Monitoring & Metrics** with Prometheus integration
- **Comprehensive Error Handling** with standardized responses
- **OOP Design Principles** with modular architecture

## 📋 Prerequisites

- **Java 17** or higher (Compatible with Mac M2 ARM64)
- **Maven 3.8+** for dependency management
- **Git** for version control

## 🛠️ Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd Yagesh
```

### 2. Build the Project
```bash
mvn clean install
```

### 3. Run the Application
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 4. Access the Dashboard
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console
- **Actuator Health**: http://localhost:8080/actuator/health
- **Prometheus Metrics**: http://localhost:8080/actuator/prometheus

## 🔐 Default Credentials

### Admin User
- **Username**: `admin`
- **Password**: `admin123`
- **Roles**: ADMIN, USER

### Operator User
- **Username**: `operator`
- **Password**: `operator123`
- **Roles**: USER

## 📚 API Documentation

### Authentication

#### Login
```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

Response:
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "type": "Bearer",
    "userId": "uuid",
    "username": "admin",
    "roles": ["ADMIN", "USER"]
  }
}
```

### Alert Management

#### Create Alert
```bash
POST /api/v1/alerts
Authorization: Bearer <token>
Content-Type: application/json

{
  "alertType": "OVERSPEEDING",
  "severity": "WARNING",
  "driverId": "D001",
  "vehicleId": "V001",
  "metadata": {
    "speed": 85,
    "limit": 60,
    "location": "Highway 101"
  }
}
```

#### Get Active Alerts
```bash
GET /api/v1/alerts/active
Authorization: Bearer <token>
```

#### Resolve Alert
```bash
PUT /api/v1/alerts/{alertId}/resolve
Authorization: Bearer <token>
Content-Type: application/json

{
  "reason": "Driver counseled and acknowledged violation"
}
```

### Dashboard

#### Get Dashboard Overview
```bash
GET /api/v1/dashboard/overview
Authorization: Bearer <token>
```

#### Get Alert Trends
```bash
GET /api/v1/dashboard/trends?days=7
Authorization: Bearer <token>
```

#### Get Recently Auto-Closed Alerts
```bash
GET /api/v1/dashboard/auto-closed?hours=24
Authorization: Bearer <token>
```

## 🎯 Use Cases & Demo Scenarios

### Scenario 1: Overspeeding Alert Escalation

**Rule**: Escalate to CRITICAL if 3 overspeeding alerts within 1 hour

```bash
# Create 1st overspeeding alert
POST /api/v1/alerts
{
  "alertType": "OVERSPEEDING",
  "severity": "WARNING",
  "driverId": "D001",
  "metadata": {"speed": 85}
}

# Create 2nd overspeeding alert (same driver, within 1 hour)
POST /api/v1/alerts
{
  "alertType": "OVERSPEEDING",
  "severity": "WARNING",
  "driverId": "D001",
  "metadata": {"speed": 90}
}

# Create 3rd overspeeding alert (same driver, within 1 hour)
POST /api/v1/alerts
{
  "alertType": "OVERSPEEDING",
  "severity": "WARNING",
  "driverId": "D001",
  "metadata": {"speed": 88}
}

# All three alerts will automatically escalate to CRITICAL
```

### Scenario 2: Compliance Document Auto-Closure

**Rule**: Auto-close when document is renewed

```bash
# Create compliance alert
POST /api/v1/alerts
{
  "alertType": "COMPLIANCE_DOCUMENT_EXPIRY",
  "severity": "WARNING",
  "driverId": "D002",
  "metadata": {"documentType": "license", "expiryDate": "2024-01-15"}
}

# Update alert with condition (document renewed)
PATCH /api/v1/alerts/{alertId}/condition?condition=DOCUMENT_RENEWED

# Alert will automatically close
```

### Scenario 3: Negative Feedback Escalation

**Rule**: Escalate if 2 negative feedbacks within 24 hours

```bash
# Create 1st negative feedback
POST /api/v1/alerts
{
  "alertType": "FEEDBACK_NEGATIVE",
  "severity": "INFO",
  "driverId": "D003",
  "metadata": {"rating": 1, "comment": "Rude behavior"}
}

# Create 2nd negative feedback (same driver, within 24 hours)
POST /api/v1/alerts
{
  "alertType": "FEEDBACK_NEGATIVE",
  "severity": "INFO",
  "driverId": "D003",
  "metadata": {"rating": 2, "comment": "Late arrival"}
}

# Both alerts escalate to CRITICAL
```

## ⚙️ Configuration

### Rules Configuration (`src/main/resources/rules.json`)

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
    }
  ]
}
```

### Application Configuration (`src/main/resources/application.yml`)

Key configurations:
- **Server Port**: 8080
- **Database**: H2 (in-memory) - can be switched to PostgreSQL
- **JWT Secret**: Configurable in application.yml
- **Cache TTL**: 5-10 minutes
- **Auto-close Schedule**: Every 5 minutes (cron: `0 */5 * * * *`)
- **Data Retention**: 90 days

## 🏗️ Architecture

### System Components

```
┌─────────────────────────────────────────────────────────────┐
│                    REST API Layer                            │
│  (AlertController, DashboardController, AuthController)      │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    Service Layer                             │
│  (AlertService, RuleEvaluationService, DashboardService)     │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    Rule Engine                               │
│  (RuleEngine, RuleLoader - DSL Support)                      │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    Data Layer                                │
│  (JPA Repositories, Entities)                                │
└─────────────────────────────────────────────────────────────┘
```

### Alert Lifecycle

```
OPEN → ESCALATED → AUTO_CLOSED
  ↓                      ↑
  └────→ RESOLVED ←──────┘
```

### Background Jobs

1. **AutoCloseScheduler**: Runs every 5 minutes to evaluate and auto-close eligible alerts
2. **DataRetentionScheduler**: Runs daily at 2 AM to clean up old data
3. **RuleReloadScheduler**: (Optional) Hot-reload rules without restart

## 📊 Performance & Complexity Analysis

### Time Complexity

| Operation | Complexity | Notes |
|-----------|-----------|-------|
| Create Alert | O(1) | Single insert + rule evaluation |
| Get Alert by ID | O(1) | With cache hit, O(log n) with DB |
| Find Active Alerts | O(n) | n = number of active alerts |
| Rule Evaluation | O(m) | m = alerts in time window |
| Auto-close Job | O(n * m) | n = active alerts, m = batch size |
| Dashboard Queries | O(n) | Cached for 5 minutes |

### Space Complexity

| Component | Space | Notes |
|-----------|-------|-------|
| Rules Cache | O(r) | r = number of rules (~10-20) |
| Alerts Cache | O(1000) | Max 1000 entries, TTL 10 min |
| Dashboard Cache | O(50) | Max 50 entries, TTL 5 min |
| Database | O(n) | n = total alerts (with retention) |

### Performance Optimizations

1. **Database Indexing**: Composite indexes on frequently queried columns
2. **Caching**: Caffeine cache for hot data
3. **Batch Processing**: Auto-close processes in configurable batches
4. **Connection Pooling**: HikariCP for optimal DB connections
5. **Lazy Loading**: JPA lazy loading for related entities

## 🔍 Monitoring & Observability

### Metrics Available

- `alerts.created` - Total alerts created
- `alerts.escalated` - Total alerts escalated
- `alerts.autoclosed` - Total alerts auto-closed
- `alerts.resolved` - Total alerts manually resolved
- `rules.evaluations` - Total rule evaluations
- `alerts.creation.time` - Alert creation latency
- `rules.evaluation.time` - Rule evaluation latency

### Health Checks

```bash
GET /actuator/health

Response:
{
  "status": "UP",
  "components": {
    "alertSystemHealth": {
      "status": "UP",
      "details": {
        "rulesLoaded": 6,
        "status": "Operational"
      }
    }
  }
}
```

### Logging

Structured logging with different levels:
- **DEBUG**: Detailed flow information
- **INFO**: Operational events (alert created, escalated, etc.)
- **WARN**: Non-critical issues (validation failures, etc.)
- **ERROR**: Critical failures requiring attention

Logs are written to:
- Console (for development)
- File: `logs/alert-system.log` (rolling, max 10MB, 30 days retention)

## 🛡️ Security Features

1. **JWT Authentication**: Stateless token-based auth
2. **Password Encryption**: BCrypt with salt
3. **CORS Configuration**: Configurable allowed origins
4. **SQL Injection Prevention**: JPA prepared statements
5. **Input Validation**: Bean Validation (JSR-303)
6. **Error Message Sanitization**: No sensitive data in errors

## ⚖️ Trade-offs & Design Decisions

### 1. H2 vs PostgreSQL
**Decision**: Default to H2 for ease of setup
- ✅ **Pros**: Zero configuration, fast development
- ❌ **Cons**: Not for production, data loss on restart
- **Mitigation**: Easy switch to PostgreSQL via configuration

### 2. In-Memory Cache vs Redis
**Decision**: Caffeine (in-memory) cache
- ✅ **Pros**: Low latency, no external dependency
- ❌ **Cons**: Not distributed, lost on restart
- **Mitigation**: Short TTL, can add Redis for production

### 3. Synchronous vs Asynchronous Rule Evaluation
**Decision**: Synchronous evaluation on alert creation
- ✅ **Pros**: Immediate feedback, simpler logic
- ❌ **Cons**: Slightly higher latency on create
- **Mitigation**: Fast evaluation (< 50ms), cached rules

### 4. Polling vs Event-Driven Architecture
**Decision**: Polling-based auto-close scheduler
- ✅ **Pros**: Simpler implementation, reliable
- ❌ **Cons**: Slight delay (up to 5 minutes)
- **Mitigation**: Configurable frequency, acceptable for use case

### 5. Monolithic vs Microservices
**Decision**: Monolithic architecture
- ✅ **Pros**: Easier development, single deployment
- ❌ **Cons**: Scaling complexity
- **Mitigation**: Modular design, can split later

## 🧪 Testing

### Run Tests
```bash
mvn test
```

### Test Coverage
- Unit tests for services and rule engine
- Integration tests for API endpoints
- Mock-based testing for external dependencies

## 🚢 Deployment

### Production Configuration

1. **Switch to PostgreSQL**:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/alertdb
    username: dbuser
    password: dbpass
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

2. **Enable Redis Cache** (Optional):
```yaml
spring:
  cache:
    type: redis
  redis:
    host: localhost
    port: 6379
```

3. **Configure JWT Secret**:
```yaml
jwt:
  secret: <your-long-secure-secret-key>
```

### Docker Deployment

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
# Build
docker build -t alert-system:1.0.0 .

# Run
docker run -p 8080:8080 alert-system:1.0.0
```

## 📝 Future Enhancements

1. **Real-time Notifications**: WebSocket support for live updates
2. **Advanced Analytics**: ML-based prediction for alert patterns
3. **Multi-tenancy**: Support for multiple organizations
4. **Mobile App**: Native iOS/Android apps
5. **Geo-fencing**: Location-based alert rules
6. **Integration Hub**: Connectors for external systems
7. **Custom Workflows**: User-defined alert resolution workflows
8. **Advanced Reporting**: PDF/Excel export with charts

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

This project is licensed under the Apache License 2.0

## 👥 Contact

For questions or support:
- **Email**: support@moveinsync.com
- **Documentation**: [API Docs](http://localhost:8080/swagger-ui.html)

---

**Built with ❤️ for MoveInSync Fleet Management**

