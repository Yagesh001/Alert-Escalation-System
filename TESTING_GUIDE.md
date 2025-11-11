# Testing Guide

This guide provides comprehensive instructions for testing the Intelligent Alert Escalation & Resolution System.

## Table of Contents
1. [Test Scenarios](#test-scenarios)
2. [Functional Requirements Validation](#functional-requirements-validation)
3. [Rule Engine Testing](#rule-engine-testing)
4. [Dashboard Testing](#dashboard-testing)
5. [Performance Testing](#performance-testing)
6. [Security Testing](#security-testing)

---

## Test Scenarios

### Scenario 1: Overspeeding Alert Escalation

**Objective**: Verify that 3 overspeeding alerts within 1 hour trigger escalation to CRITICAL.

**Rule Configuration**:
```json
{
  "alertType": "OVERSPEEDING",
  "escalateIfCount": 3,
  "windowMinutes": 60,
  "escalationSeverity": "CRITICAL"
}
```

**Test Steps**:

1. **Create First Alert** (Time: T+0)
```bash
POST /api/v1/alerts
{
  "alertType": "OVERSPEEDING",
  "severity": "WARNING",
  "driverId": "TEST_DRIVER_001",
  "metadata": {"speed": 85, "limit": 60}
}
```

**Expected**: Alert created with status `OPEN`, severity `WARNING`

2. **Create Second Alert** (Time: T+5 minutes)
```bash
POST /api/v1/alerts
{
  "alertType": "OVERSPEEDING",
  "severity": "WARNING",
  "driverId": "TEST_DRIVER_001",
  "metadata": {"speed": 88, "limit": 60}
}
```

**Expected**: Alert created with status `OPEN`, severity `WARNING`

3. **Create Third Alert** (Time: T+10 minutes)
```bash
POST /api/v1/alerts
{
  "alertType": "OVERSPEEDING",
  "severity": "WARNING",
  "driverId": "TEST_DRIVER_001",
  "metadata": {"speed": 92, "limit": 60}
}
```

**Expected**:
- ✅ All three alerts escalated to status `ESCALATED`
- ✅ All three alerts have severity `CRITICAL`
- ✅ Escalation reason logged: "3 occurrences of OVERSPEEDING within X minutes"
- ✅ Alert history entries created for each escalation

4. **Verify Escalation**
```bash
GET /api/v1/alerts/driver/TEST_DRIVER_001
```

**Expected Response**:
```json
{
  "success": true,
  "data": [
    {
      "status": "ESCALATED",
      "severity": "CRITICAL",
      "escalatedAt": "2024-01-15T10:10:30",
      "escalationReason": "3 occurrences of OVERSPEEDING within 10 minutes..."
    }
    // ... all 3 alerts
  ]
}
```

**Edge Cases to Test**:
- ❌ Only 2 alerts within 1 hour (should NOT escalate)
- ❌ 3 alerts but spanning > 1 hour (should NOT escalate)
- ✅ 4th alert after escalation (already escalated, no change)

---

### Scenario 2: Compliance Document Auto-Closure

**Objective**: Verify that compliance alert auto-closes when document is renewed.

**Rule Configuration**:
```json
{
  "alertType": "COMPLIANCE_DOCUMENT_EXPIRY",
  "autoCloseIf": "DOCUMENT_RENEWED"
}
```

**Test Steps**:

1. **Create Compliance Alert**
```bash
POST /api/v1/alerts
{
  "alertType": "COMPLIANCE_DOCUMENT_EXPIRY",
  "severity": "WARNING",
  "driverId": "TEST_DRIVER_002",
  "metadata": {"documentType": "license", "expiryDate": "2024-01-15"}
}
```

**Expected**: Alert created with status `OPEN`

2. **Update Alert Condition** (Document Renewed)
```bash
PATCH /api/v1/alerts/{alertId}/condition?condition=DOCUMENT_RENEWED
```

**Expected**:
- ✅ Alert status changed to `AUTO_CLOSED`
- ✅ Closure reason: "Condition met: DOCUMENT_RENEWED"
- ✅ `closedAt` timestamp populated
- ✅ `closedBy` = "SYSTEM"
- ✅ History entry created with event type "AUTO_CLOSED"

3. **Verify Closure**
```bash
GET /api/v1/alerts/{alertId}
```

**Expected Response**:
```json
{
  "status": "AUTO_CLOSED",
  "closedAt": "2024-01-15T14:30:00",
  "closureReason": "Condition met: DOCUMENT_RENEWED",
  "closedBy": "SYSTEM"
}
```

---

### Scenario 3: Negative Feedback Escalation

**Objective**: Verify that 2 negative feedbacks within 24 hours trigger escalation.

**Rule Configuration**:
```json
{
  "alertType": "FEEDBACK_NEGATIVE",
  "escalateIfCount": 2,
  "windowMinutes": 1440,
  "escalationSeverity": "CRITICAL"
}
```

**Test Steps**:

1. **Create First Feedback Alert**
```bash
POST /api/v1/alerts
{
  "alertType": "FEEDBACK_NEGATIVE",
  "severity": "INFO",
  "driverId": "TEST_DRIVER_003",
  "metadata": {"rating": 1, "comment": "Rude behavior"}
}
```

2. **Create Second Feedback Alert** (within 24 hours)
```bash
POST /api/v1/alerts
{
  "alertType": "FEEDBACK_NEGATIVE",
  "severity": "INFO",
  "driverId": "TEST_DRIVER_003",
  "metadata": {"rating": 2, "comment": "Late arrival"}
}
```

**Expected**:
- ✅ Both alerts escalated to `CRITICAL`
- ✅ Both alerts have status `ESCALATED`

---

### Scenario 4: Manual Alert Resolution

**Objective**: Verify that alerts can be manually resolved by authorized users.

**Test Steps**:

1. **Create Alert**
```bash
POST /api/v1/alerts
{
  "alertType": "OVERSPEEDING",
  "severity": "WARNING",
  "driverId": "TEST_DRIVER_004"
}
```

2. **Resolve Alert** (as admin user)
```bash
PUT /api/v1/alerts/{alertId}/resolve
{
  "reason": "Driver counseled and acknowledged violation"
}
```

**Expected**:
- ✅ Alert status changed to `RESOLVED`
- ✅ `closedAt` timestamp populated
- ✅ `closedBy` = authenticated user ID
- ✅ `closureReason` = provided reason
- ✅ History entry created

3. **Attempt to Resolve Again** (idempotency test)
```bash
PUT /api/v1/alerts/{alertId}/resolve
{
  "reason": "Another reason"
}
```

**Expected**: No error, alert remains `RESOLVED` (idempotent)

---

## Functional Requirements Validation

### Requirement 1: Centralized Alert Management

**Test**: Alerts from multiple source types

```bash
# Safety Module
POST /api/v1/alerts {"alertType": "OVERSPEEDING", ...}
POST /api/v1/alerts {"alertType": "HARSH_BRAKING", ...}

# Compliance Module
POST /api/v1/alerts {"alertType": "COMPLIANCE_DOCUMENT_EXPIRY", ...}

# Feedback Module
POST /api/v1/alerts {"alertType": "FEEDBACK_NEGATIVE", ...}
```

**Verification**:
```bash
GET /api/v1/alerts/active
```

**Expected**: All alerts unified in common format with required fields:
- ✅ `alertId`
- ✅ `sourceType` (alertType)
- ✅ `severity`
- ✅ `timestamp`
- ✅ `status`
- ✅ `metadata`

### Requirement 2: Rule Engine

**Test**: Dynamic rule evaluation

1. **Verify Rules Loaded**
```bash
GET /actuator/health
```

Look for: `"rulesLoaded": 6`

2. **Test Each Rule Type**:
- OVERSPEEDING (3 in 60 min)
- FEEDBACK_NEGATIVE (2 in 1440 min)
- COMPLIANCE_DOCUMENT_EXPIRY (condition-based)
- HARSH_BRAKING (5 in 120 min)
- ROUTE_DEVIATION (2 in 30 min)

### Requirement 3: Auto-Close Background Job

**Test**: Wait for scheduled execution (every 5 minutes)

**Verify in Logs**:
```
Auto-close job completed: processed=X, closed=Y, duration=Zms
```

**Manual Trigger** (if available):
- Add admin endpoint to trigger manually for testing

### Requirement 4: Dashboard

**Test All Dashboard Endpoints**:

```bash
# Overview
GET /api/v1/dashboard/overview

# Trends
GET /api/v1/dashboard/trends?days=7

# Auto-closed
GET /api/v1/dashboard/auto-closed?hours=24

# Statistics
GET /api/v1/dashboard/statistics

# Drill-down
GET /api/v1/dashboard/alert/{alertId}/drilldown
```

**Expected Data**:
- ✅ Severity counts (CRITICAL, WARNING, INFO)
- ✅ Top 5 drivers with most alerts
- ✅ Recently auto-closed alerts with reason
- ✅ Recent activity stream
- ✅ Trend data over time

---

## Rule Engine Testing

### Test: Rule Loading

```bash
# Check health
GET /actuator/health

# Expected
{
  "alertSystemHealth": {
    "status": "UP",
    "rulesLoaded": 6
  }
}
```

### Test: Rule Evaluation Performance

**Benchmark**: Rule evaluation should complete in < 50ms

Create 10 alerts rapidly and measure response time:

```bash
for i in {1..10}; do
  time curl -X POST http://localhost:8080/api/v1/alerts \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"alertType": "OVERSPEEDING", "severity": "WARNING", "driverId": "PERF_TEST"}'
done
```

**Target**: Each request < 150ms (including rule evaluation)

### Test: Rule Idempotency

**Scenario**: Escalate same set of alerts twice

**Expected**: Escalation occurs only once, no duplicate history entries

---

## Dashboard Testing

### Test: Top Offenders

**Setup**: Create multiple alerts for different drivers

```bash
# Driver A: 5 alerts
# Driver B: 3 alerts
# Driver C: 2 alerts
```

**Query**:
```bash
GET /api/v1/dashboard/overview
```

**Expected**: Top drivers list shows Driver A first, then B, then C

### Test: Auto-Closed Transparency

**Setup**: Create and auto-close alerts via condition

**Query**:
```bash
GET /api/v1/dashboard/auto-closed?hours=24
```

**Expected**:
- ✅ Shows recently auto-closed alerts
- ✅ Includes closure reason
- ✅ Filters by time range correctly

### Test: Alert Drill-Down

**Query**:
```bash
GET /api/v1/dashboard/alert/{alertId}/drilldown
```

**Expected**:
- ✅ Complete alert details
- ✅ Full history timeline (OPEN → ESCALATED → CLOSED)
- ✅ All metadata fields
- ✅ State transition timestamps

---

## Performance Testing

### Load Test: Alert Creation

**Tool**: Apache Bench or JMeter

```bash
# 1000 requests, 10 concurrent
ab -n 1000 -c 10 -H "Authorization: Bearer $TOKEN" \
   -p alert.json -T application/json \
   http://localhost:8080/api/v1/alerts
```

**Targets**:
- Throughput: > 500 req/sec
- 95th percentile latency: < 200ms
- Error rate: < 1%

### Load Test: Dashboard Queries

```bash
ab -n 1000 -c 10 -H "Authorization: Bearer $TOKEN" \
   http://localhost:8080/api/v1/dashboard/overview
```

**Targets**:
- 95th percentile latency: < 300ms (with cache)
- Cache hit ratio: > 70%

### Stress Test: Auto-Close Job

**Setup**: Create 1000 active alerts

**Measure**:
- Job execution time (should be < 60 seconds)
- Memory usage (should not spike)
- CPU usage (should be < 80%)

---

## Security Testing

### Test: Authentication Required

**Without Token**:
```bash
curl -X GET http://localhost:8080/api/v1/alerts/active
```

**Expected**: 401 Unauthorized

### Test: Invalid Token

```bash
curl -X GET http://localhost:8080/api/v1/alerts/active \
  -H "Authorization: Bearer INVALID_TOKEN"
```

**Expected**: 401 Unauthorized

### Test: Token Expiration

1. Login and get token
2. Wait 24+ hours (or modify JWT expiration for testing)
3. Use expired token

**Expected**: 401 Unauthorized

### Test: Input Validation

**Invalid Alert Type**:
```bash
POST /api/v1/alerts
{
  "alertType": "INVALID_TYPE",
  "severity": "WARNING"
}
```

**Expected**: 400 Bad Request with validation error

**Missing Required Field**:
```bash
POST /api/v1/alerts
{
  "alertType": "OVERSPEEDING"
  # Missing severity
}
```

**Expected**: 400 Bad Request

### Test: SQL Injection Prevention

**Attempt SQL Injection**:
```bash
GET /api/v1/alerts/driver/D001'; DROP TABLE alerts;--
```

**Expected**: No SQL execution, safe parameter handling

---

## Automated Test Suite

### Unit Tests

```bash
mvn test
```

**Coverage Targets**:
- Services: > 80%
- Rule Engine: > 90%
- Controllers: > 70%

### Integration Tests

```bash
mvn verify
```

**Test Cases**:
- API endpoint integration
- Database operations
- Security filters
- Caching behavior

---

## Test Data Setup

### Quick Test Data Script

```bash
#!/bin/bash
TOKEN="YOUR_TOKEN_HERE"
BASE_URL="http://localhost:8080/api/v1"

# Create diverse alerts
curl -X POST $BASE_URL/alerts -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"alertType": "OVERSPEEDING", "severity": "WARNING", "driverId": "D001"}'

curl -X POST $BASE_URL/alerts -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"alertType": "HARSH_BRAKING", "severity": "INFO", "driverId": "D002"}'

curl -X POST $BASE_URL/alerts -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"alertType": "FEEDBACK_NEGATIVE", "severity": "INFO", "driverId": "D003"}'

echo "Test data created successfully"
```

---

## Checklist

Use this checklist to ensure comprehensive testing:

### Functional Requirements
- [ ] Alerts created from multiple source types
- [ ] Alerts stored with unified format
- [ ] Alert state transitions work (OPEN → ESCALATED → AUTO_CLOSED/RESOLVED)
- [ ] Rule-based escalation works for all configured rules
- [ ] Auto-close by condition works
- [ ] Auto-close by time window works
- [ ] Manual resolution works
- [ ] Idempotency maintained for all operations

### Dashboard Requirements
- [ ] Dashboard overview shows all metrics
- [ ] Top 5 drivers displayed correctly
- [ ] Recent auto-closed alerts shown with reasons
- [ ] Trend data accurate over time
- [ ] Alert drill-down shows complete history
- [ ] Filters work (time range, status, etc.)

### Non-Functional Requirements
- [ ] Authentication working (JWT)
- [ ] Authorization enforced
- [ ] Caching improves performance
- [ ] Background jobs run on schedule
- [ ] Error handling returns meaningful messages
- [ ] Monitoring metrics recorded
- [ ] Logs generated appropriately
- [ ] API documentation accessible

---

## Reporting Issues

When reporting test failures, include:
1. Test scenario name
2. Steps to reproduce
3. Expected behavior
4. Actual behavior
5. Relevant logs
6. System configuration (OS, Java version, etc.)

---

**Happy Testing! 🎯**

