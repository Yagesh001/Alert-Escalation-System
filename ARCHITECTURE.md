# System Architecture & Design

## Table of Contents
1. [Overview](#overview)
2. [Design Principles](#design-principles)
3. [Component Architecture](#component-architecture)
4. [Data Flow](#data-flow)
5. [Database Schema](#database-schema)
6. [Security Architecture](#security-architecture)
7. [Caching Strategy](#caching-strategy)
8. [Scalability Considerations](#scalability-considerations)

## Overview

The Intelligent Alert Escalation & Resolution System is built using **Java 17** and **Spring Boot 3.2**, following a **layered architecture** pattern with clear separation of concerns.

### Key Architectural Patterns

1. **Layered Architecture**: Clear separation between presentation, business, and data layers
2. **Repository Pattern**: Data access abstraction
3. **Service Layer Pattern**: Business logic encapsulation
4. **Strategy Pattern**: Rule engine implementation
5. **Factory Pattern**: Entity creation
6. **Builder Pattern**: Complex object construction

## Design Principles

### SOLID Principles

#### 1. Single Responsibility Principle (SRP)
- **AlertService**: Manages alert CRUD operations
- **RuleEvaluationService**: Handles rule evaluation logic
- **DashboardService**: Provides analytics and aggregations
- **RuleEngine**: Evaluates escalation and auto-closure rules

#### 2. Open/Closed Principle (OCP)
- **Rule Engine**: Open for extension (new rule types) without modifying core logic
- **Alert Types**: New alert types can be added via enum extension
- **Exception Handling**: Global handler extensible for new exception types

#### 3. Liskov Substitution Principle (LSP)
- **Repository Interfaces**: All implementations are substitutable
- **UserDetailsService**: Custom implementation substitutable with Spring Security defaults

#### 4. Interface Segregation Principle (ISP)
- **Focused Repositories**: Each repository has only relevant methods
- **Service Interfaces**: Services expose only required operations

#### 5. Dependency Inversion Principle (DIP)
- **Dependency Injection**: All dependencies injected via constructor
- **Abstractions**: Services depend on repository interfaces, not implementations

### Other Key Principles

- **DRY (Don't Repeat Yourself)**: Reusable components and utilities
- **KISS (Keep It Simple, Stupid)**: Simple, understandable implementations
- **YAGNI (You Aren't Gonna Need It)**: No speculative features

## Component Architecture

### Layer Structure

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Alert      │  │  Dashboard   │  │     Auth     │      │
│  │  Controller  │  │  Controller  │  │  Controller  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      Business Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Alert      │  │     Rule     │  │  Dashboard   │      │
│  │   Service    │  │  Evaluation  │  │   Service    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                               │
│  ┌─────────────────────────────────────────────────┐        │
│  │            Rule Engine & Loader                  │        │
│  └─────────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                       Data Layer                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Alert      │  │AlertHistory  │  │     User     │      │
│  │  Repository  │  │  Repository  │  │  Repository  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                       Database (H2/PostgreSQL)               │
└─────────────────────────────────────────────────────────────┘
```

### Cross-Cutting Concerns

```
┌─────────────────────────────────────────────────────────────┐
│                   Security (JWT Filter)                      │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│              Exception Handling (Global Handler)             │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│                  Caching (Caffeine)                          │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│              Monitoring (Micrometer/Prometheus)              │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│              Logging (SLF4J/Logback)                         │
└─────────────────────────────────────────────────────────────┘
```

## Data Flow

### Alert Creation Flow

```
1. External System → POST /api/v1/alerts
                ↓
2. AuthenticationFilter → Validate JWT
                ↓
3. AlertController → Validate Request
                ↓
4. AlertService → Create Alert Entity
                ↓
5. AlertRepository → Save to Database
                ↓
6. AlertHistoryRepository → Create History Entry
                ↓
7. RuleEvaluationService → Evaluate Rules (Async)
                ↓
8. RuleEngine → Check Escalation Conditions
                ↓
9. If escalation needed:
   - Update Alert Status → ESCALATED
   - Update Severity
   - Create History Entry
                ↓
10. CacheManager → Evict Affected Caches
                ↓
11. Return AlertResponse to Client
```

### Rule Evaluation Flow

```
1. New Alert Created
                ↓
2. Get Rule for Alert Type (Cached)
                ↓
3. Fetch Recent Alerts (Same Type, Same Driver, Time Window)
                ↓
4. Count Alerts in Window
                ↓
5. Check if Count >= Threshold
                ↓
6. If Yes:
   - Calculate Time Difference
   - Check if Within Window
   - Escalate All Alerts in Window
                ↓
7. If No:
   - No action needed
                ↓
8. Record Metrics
```

### Auto-Close Background Job Flow

```
1. Scheduler Triggered (Every 5 Minutes)
                ↓
2. Prevent Overlapping Execution (AtomicBoolean)
                ↓
3. Fetch All Active Alerts (OPEN/ESCALATED)
                ↓
4. Process in Batches (Default: 100)
                ↓
5. For Each Alert:
   - Get Rule for Alert Type
   - Check Auto-Close Conditions
   - If Met: Update Status → AUTO_CLOSED
   - Create History Entry
                ↓
6. Record Metrics (Processed, Closed, Duration)
                ↓
7. Release Lock
```

## Database Schema

### Entity Relationship Diagram

```
┌──────────────────┐
│      User        │
├──────────────────┤
│ PK user_id       │
│    username      │
│    email         │
│    password      │
│    roles         │
└──────────────────┘

┌──────────────────┐       ┌──────────────────┐
│      Alert       │       │  AlertHistory    │
├──────────────────┤       ├──────────────────┤
│ PK alert_id      │───<───│ FK alert_id      │
│    alert_type    │       │    from_status   │
│    severity      │       │    to_status     │
│    status        │       │    timestamp     │
│    timestamp     │       │    reason        │
│    driver_id     │       │    changed_by    │
│    vehicle_id    │       │    event_type    │
│    metadata      │       └──────────────────┘
│    escalated_at  │
│    closed_at     │
└──────────────────┘
```

### Key Indexes

1. **alerts.idx_alert_status**: On `status` column (for active alert queries)
2. **alerts.idx_alert_type**: On `alertType` column (for type-based queries)
3. **alerts.idx_alert_driver**: On `driverId` column (for driver-specific queries)
4. **alerts.idx_alert_timestamp**: On `timestamp` column (for time-based queries)
5. **alert_history.idx_history_alert**: On `alertId` column (for history lookups)

### Database Considerations

#### H2 (Development)
- **Pros**: Zero config, fast startup, good for testing
- **Cons**: In-memory, data lost on restart
- **Use Case**: Local development, testing

#### PostgreSQL (Production)
- **Pros**: ACID compliance, scalable, reliable
- **Cons**: Requires setup, external dependency
- **Use Case**: Production deployment

## Security Architecture

### Authentication Flow

```
1. User → POST /api/v1/auth/login (username, password)
              ↓
2. AuthService → Validate Credentials (BCrypt)
              ↓
3. JwtTokenProvider → Generate JWT Token
              ↓
4. Return Token to User
              ↓
5. User includes token in subsequent requests:
   Authorization: Bearer <token>
              ↓
6. JwtAuthenticationFilter → Intercept Request
              ↓
7. Extract and Validate Token
              ↓
8. Load UserDetails
              ↓
9. Set SecurityContext
              ↓
10. Proceed to Controller
```

### Security Layers

1. **Transport Security**: HTTPS in production
2. **Authentication**: JWT tokens with 24-hour expiration
3. **Authorization**: Role-based access control (RBAC)
4. **Password Security**: BCrypt with salt
5. **SQL Injection Prevention**: JPA prepared statements
6. **XSS Prevention**: Input sanitization
7. **CSRF Protection**: Stateless JWT (no CSRF needed)

## Caching Strategy

### Cache Hierarchy

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Cache (Caffeine)              │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │    Alerts    │  │    Rules     │  │  Dashboard   │      │
│  │  Cache       │  │   Cache      │  │   Cache      │      │
│  │              │  │              │  │              │      │
│  │ TTL: 10 min  │  │ TTL: 5 min   │  │ TTL: 5 min   │      │
│  │ Max: 1000    │  │ Max: 100     │  │ Max: 50      │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                               │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                        Database                              │
└─────────────────────────────────────────────────────────────┘
```

### Cache Invalidation Strategy

1. **Time-based**: Automatic expiration (TTL)
2. **Event-based**: Eviction on updates
   - Alert created → Evict alerts cache
   - Alert updated → Evict alerts + dashboard cache
   - Alert closed → Evict all caches

### Cache Hit Ratio Optimization

- **Hot Data**: Frequently accessed data (active alerts, rules)
- **Cold Data**: Historical data (not cached)
- **Warm-up**: Pre-load rules on startup

## Scalability Considerations

### Vertical Scaling

- **JVM Heap**: Increase for larger cache
- **Connection Pool**: Increase for higher concurrency
- **Thread Pool**: Increase for background jobs

### Horizontal Scaling

#### Current Limitations
1. **Scheduler**: Multiple instances will run duplicate jobs
2. **Cache**: Not distributed (per-instance cache)

#### Solutions for Horizontal Scaling

1. **Distributed Scheduler**:
   - Use **ShedLock** or **Quartz** with DB persistence
   - Only one instance runs job at a time

2. **Distributed Cache**:
   - Replace Caffeine with **Redis**
   - Shared cache across instances

3. **Database**:
   - Connection pooling per instance
   - Read replicas for queries
   - Write to master only

4. **Load Balancer**:
   - NGINX or AWS ALB
   - Sticky sessions (for WebSocket future)
   - Health checks

### Performance Benchmarks

| Operation | Target Latency | Expected Throughput |
|-----------|---------------|---------------------|
| Create Alert | < 100ms | 1000 req/sec |
| Get Alert (cached) | < 10ms | 10000 req/sec |
| Get Dashboard | < 200ms | 500 req/sec |
| Rule Evaluation | < 50ms | N/A (async) |
| Auto-close Job | < 30sec | N/A (batch) |

## Error Handling Strategy

### Error Categories

1. **Client Errors (4xx)**:
   - 400: Validation failures
   - 401: Authentication failures
   - 404: Resource not found

2. **Server Errors (5xx)**:
   - 500: Unexpected errors
   - 503: Service unavailable

### Error Response Format

```json
{
  "success": false,
  "message": "Error description",
  "timestamp": "2024-01-15T10:30:00"
}
```

### Failure Recovery

1. **Database Failures**: Retry with exponential backoff
2. **Rule Evaluation Failures**: Log and continue (don't fail alert creation)
3. **Auto-close Job Failures**: Log and retry next cycle

## Monitoring & Observability

### Metrics Collection

- **Application Metrics**: Custom metrics via Micrometer
- **JVM Metrics**: Heap, GC, threads
- **Database Metrics**: Connection pool, query performance
- **Cache Metrics**: Hit ratio, evictions

### Alerting

Recommended alerts:
1. Alert creation rate spike
2. Auto-close job duration > 1 minute
3. Cache hit ratio < 70%
4. Database connection pool exhaustion
5. Error rate > 5%

### Tracing

- **Request ID**: Generated per request
- **MDC Logging**: Contextual logging with request ID
- **Distributed Tracing**: Future: OpenTelemetry/Jaeger

---

## Summary

This architecture provides:
- ✅ **Modularity**: Easy to maintain and extend
- ✅ **Testability**: Clear separation enables unit testing
- ✅ **Performance**: Optimized with caching and indexing
- ✅ **Security**: Multiple layers of protection
- ✅ **Scalability**: Can scale horizontally with minor changes
- ✅ **Observability**: Comprehensive monitoring and logging

The system is production-ready with considerations for future enhancements and scaling requirements.

