# Quick Start Guide

Get the Intelligent Alert System running in less than 5 minutes!

## Prerequisites

- Java 17+ installed (works on Mac M2 Air)
- Maven 3.8+
- Your favorite REST client (Postman, cURL, or use Swagger UI)

## Step 1: Build and Run (2 minutes)

```bash
# Navigate to project directory
cd Yagesh

# Build the project
mvn clean package -DskipTests

# Run the application
mvn spring-boot:run
```

You should see:
```
Intelligent Alert Escalation & Resolution System started successfully
```

## Step 2: Verify Installation (30 seconds)

Open your browser and check:

✅ **Swagger UI**: http://localhost:8080/swagger-ui.html  
✅ **Health Check**: http://localhost:8080/actuator/health  
✅ **H2 Console**: http://localhost:8080/h2-console

## Step 3: Authenticate (1 minute)

### Option A: Using cURL

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

### Option B: Using Swagger UI

1. Go to http://localhost:8080/swagger-ui.html
2. Find "Authentication" section
3. Click on `POST /api/v1/auth/login`
4. Click "Try it out"
5. Use credentials: `admin` / `admin123`
6. Click "Execute"
7. Copy the token from the response

### Step 4: Set Authorization

In Swagger UI:
1. Click the "Authorize" button (🔒 icon at top)
2. Enter: `Bearer YOUR_TOKEN_HERE`
3. Click "Authorize"

You're now authenticated! ✅

## Step 5: Test Core Features (2 minutes)

### Create Your First Alert

```bash
# Replace YOUR_TOKEN with the token from Step 3
curl -X POST http://localhost:8080/api/v1/alerts \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "alertType": "OVERSPEEDING",
    "severity": "WARNING",
    "driverId": "DRIVER001",
    "vehicleId": "VEHICLE001",
    "metadata": {
      "speed": 85,
      "limit": 60,
      "location": "Highway 101"
    }
  }'
```

### View Active Alerts

```bash
curl -X GET http://localhost:8080/api/v1/alerts/active \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### View Dashboard

```bash
curl -X GET http://localhost:8080/api/v1/dashboard/overview \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Demo Scenario: Test Auto-Escalation

Let's trigger the escalation rule for overspeeding (3 alerts within 1 hour):

```bash
# Alert 1
curl -X POST http://localhost:8080/api/v1/alerts \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "alertType": "OVERSPEEDING",
    "severity": "WARNING",
    "driverId": "DRIVER_DEMO",
    "metadata": {"speed": 85, "location": "Point A"}
  }'

# Alert 2 (same driver, within 1 hour)
curl -X POST http://localhost:8080/api/v1/alerts \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "alertType": "OVERSPEEDING",
    "severity": "WARNING",
    "driverId": "DRIVER_DEMO",
    "metadata": {"speed": 90, "location": "Point B"}
  }'

# Alert 3 (same driver, within 1 hour) - This triggers escalation!
curl -X POST http://localhost:8080/api/v1/alerts \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "alertType": "OVERSPEEDING",
    "severity": "WARNING",
    "driverId": "DRIVER_DEMO",
    "metadata": {"speed": 92, "location": "Point C"}
  }'

# Check alerts - all three should now be ESCALATED with severity CRITICAL
curl -X GET http://localhost:8080/api/v1/alerts/driver/DRIVER_DEMO \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Explore More Features

### View Alert History (Audit Trail)

```bash
curl -X GET http://localhost:8080/api/v1/alerts/{ALERT_ID}/history \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Manually Resolve an Alert

```bash
curl -X PUT http://localhost:8080/api/v1/alerts/{ALERT_ID}/resolve \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Driver has been counseled and acknowledged the violation"
  }'
```

### View Top Offenders

```bash
curl -X GET http://localhost:8080/api/v1/dashboard/overview \
  -H "Authorization: Bearer YOUR_TOKEN"
```

Look for the `topDrivers` section in the response.

### View Trends

```bash
curl -X GET http://localhost:8080/api/v1/dashboard/trends?days=7 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### View Recently Auto-Closed Alerts

```bash
curl -X GET http://localhost:8080/api/v1/dashboard/auto-closed?hours=24 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Understanding the Rules

Rules are configured in `src/main/resources/rules.json`:

```json
{
  "alertType": "OVERSPEEDING",
  "escalateIfCount": 3,
  "windowMinutes": 60,
  "escalationSeverity": "CRITICAL",
  "autoCloseIfNoRepeat": true,
  "autoCloseWindowMinutes": 120
}
```

This means:
- ✅ Escalate to CRITICAL if 3+ overspeeding alerts within 60 minutes
- ✅ Auto-close if no repeat within 120 minutes

## Background Jobs

### Auto-Close Job
Runs every 5 minutes automatically. Check logs to see it in action:

```
Auto-close job completed: processed=10, closed=3, duration=250ms
```

### Data Retention Job
Runs daily at 2 AM to clean up old data (90-day retention by default).

## Monitoring & Metrics

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Prometheus Metrics
```bash
curl http://localhost:8080/actuator/prometheus
```

Look for custom metrics:
- `alerts_created_total`
- `alerts_escalated_total`
- `alerts_autoclosed_total`
- `alerts_resolved_total`

## Database Access

Access H2 console at: http://localhost:8080/h2-console

**Connection settings:**
- JDBC URL: `jdbc:h2:mem:alertdb`
- Username: `sa`
- Password: (leave blank)

Run SQL queries:
```sql
-- View all alerts
SELECT * FROM alerts;

-- View alert history
SELECT * FROM alert_history ORDER BY timestamp DESC;

-- Count by status
SELECT status, COUNT(*) FROM alerts GROUP BY status;
```

## Troubleshooting

### Port 8080 Already in Use

Change the port in `application.yml`:
```yaml
server:
  port: 8081
```

### Java Version Issues

Check your Java version:
```bash
java -version
```

Should be Java 17 or higher.

### Build Errors

Clean and rebuild:
```bash
mvn clean install -U
```

### Authentication Issues

Make sure:
1. You're including the token in the `Authorization` header
2. Token format is: `Bearer YOUR_TOKEN`
3. Token hasn't expired (24 hours)

## Next Steps

✅ Explore the complete API documentation in Swagger UI  
✅ Review `README.md` for comprehensive documentation  
✅ Check `ARCHITECTURE.md` for system design details  
✅ Read `TRADEOFFS.md` for design decisions  

## Need Help?

- Check the logs: `logs/alert-system.log`
- Review error messages in the API response
- Consult the detailed documentation files

---

**Congratulations! You now have a fully functional Intelligent Alert System running! 🎉**

