# Health Check API Documentation

## Overview

Your banking backend application now includes a comprehensive health check API that provides multiple endpoints to monitor the application's health status. The implementation includes both custom health checks and Spring Boot Actuator endpoints.

## Available Endpoints

### 1. Custom Health Check Endpoints

#### Basic Health Check
- **URL**: `/api/health`
- **Method**: GET
- **Description**: Returns comprehensive health status with component details
- **Response**: 200 OK if healthy, 503 Service Unavailable if unhealthy

```json
{
  "status": "UP",
  "version": "0.0.1-SNAPSHOT",
  "timestamp": "2024-01-15T10:30:00",
  "uptime": 120,
  "environment": "local",
  "components": {
    "database": {
      "status": "UP",
      "description": "Database connectivity check",
      "lastChecked": "2024-01-15T10:30:00",
      "responseTime": 45,
      "details": {
        "database": "PostgreSQL",
        "url": "jdbc:postgresql://localhost:5432/banking",
        "driver": "PostgreSQL JDBC Driver"
      }
    }
  }
}
```

#### Simple Health Check
- **URL**: `/api/health/simple`
- **Method**: GET
- **Description**: Returns simple OK/UNHEALTHY response
- **Response**: 200 OK with "OK" or 503 Service Unavailable with "UNHEALTHY"

#### Detailed Health Check
- **URL**: `/api/health/detailed`
- **Method**: GET
- **Description**: Returns detailed health status including JVM metrics and system information
- **Response**: 200 OK if healthy, 503 Service Unavailable if unhealthy

```json
{
  "status": "UP",
  "version": "0.0.1-SNAPSHOT",
  "timestamp": "2024-01-15T10:30:00",
  "uptime": 120,
  "environment": "local",
  "components": {
    "database": {
      "status": "UP",
      "description": "Database connectivity check",
      "lastChecked": "2024-01-15T10:30:00",
      "responseTime": 45,
      "details": {
        "database": "PostgreSQL",
        "url": "jdbc:postgresql://localhost:5432/banking"
      }
    },
    "jvm": {
      "status": "UP",
      "description": "JVM memory and system resources",
      "lastChecked": "2024-01-15T10:30:00",
      "details": {
        "maxMemory": "1024 MB",
        "totalMemory": "512 MB",
        "usedMemory": "256 MB",
        "freeMemory": "256 MB",
        "memoryUsagePercent": "25.00%",
        "availableProcessors": 8,
        "uptime": "120 seconds"
      }
    }
  },
  "systemInfo": {
    "javaVersion": "21.0.1",
    "springBootVersion": "3.5.5",
    "availableProcessors": 8,
    "maxMemory": "1024 MB",
    "totalMemory": "512 MB",
    "freeMemory": "256 MB"
  }
}
```

### 2. Spring Boot Actuator Endpoints

#### Actuator Health Check
- **URL**: `/actuator/health`
- **Method**: GET
- **Description**: Spring Boot's built-in health check with detailed component information
- **Response**: Includes database, disk space, and other built-in health indicators

#### Actuator Info
- **URL**: `/actuator/info`
- **Method**: GET
- **Description**: Application information including build details and environment info

#### Actuator Metrics
- **URL**: `/actuator/metrics`
- **Method**: GET
- **Description**: Application metrics and performance data

## Testing the Health Check API

### Using curl

```bash
# Basic health check
curl http://localhost:8080/api/health

# Simple health check
curl http://localhost:8080/api/health/simple

# Detailed health check
curl http://localhost:8080/api/health/detailed

# Spring Boot Actuator health
curl http://localhost:8080/actuator/health
```

### Using a web browser

Simply navigate to any of the URLs above in your browser to see the JSON response.

## Health Check Components

### Database Health Check
- Tests PostgreSQL database connectivity
- Executes a simple `SELECT 1` query to verify connection
- Reports connection details, response time, and any errors

### JVM Health Check
- Monitors JVM memory usage
- Reports memory statistics (max, total, used, free)
- Warns if memory usage exceeds 90%
- Provides system resource information

## HTTP Status Codes

- **200 OK**: Application is healthy
- **503 Service Unavailable**: Application is unhealthy (database down, critical errors)

## Configuration

The health check configuration is managed in `application.properties`:

```properties
# Actuator Configuration
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always
management.endpoint.health.show-components=always
management.health.defaults.enabled=true
management.health.db.enabled=true
management.health.diskspace.enabled=true
management.health.ping.enabled=true
```

## Security Considerations

- Health check endpoints are currently accessible without authentication
- For production, consider securing these endpoints or limiting access
- The detailed endpoint exposes system information that should be protected in production
- Configure `management.endpoint.health.show-details=when-authorized` for production environments

## Monitoring Integration

These health check endpoints can be easily integrated with:
- Load balancers (use `/api/health/simple` for load balancer health checks)
- Monitoring tools like Prometheus, Grafana, or New Relic
- Kubernetes liveness and readiness probes
- CI/CD pipeline health verification

## Running the Application

Start your application with:
```bash
mvn spring-boot:run -Dspring.profiles.active=local
```

The health check endpoints will be available immediately once the application starts.
