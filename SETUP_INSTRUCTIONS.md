# Setup Instructions - What You Need to Add

## ✅ Errors Fixed

All critical errors have been fixed:
- ✅ Fixed duplicate constructor in `AlertNotFoundException`
- ✅ Fixed JWT token generation (updated to new JJWT API)
- ✅ Fixed `HealthIndicator` class naming conflict
- ✅ Removed unused imports and variables
- ✅ Added `@Builder.Default` annotations

## 📋 Prerequisites You Need to Install

### 1. **Java 17** (Required)
You need Java 17 or higher installed on your Mac M2 Air.

**Check if installed:**
```bash
java -version
```

**If not installed, install using Homebrew:**
```bash
# Install Homebrew (if not already installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Java 17
brew install openjdk@17

# Link it
sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk

# Verify installation
java -version
```

### 2. **Maven 3.8+** (Required)
Maven is required to build and run the project.

**Check if installed:**
```bash
mvn -version
```

**If not installed:**
```bash
brew install maven
```

## 🚀 Quick Start (After Prerequisites)

### Step 1: Navigate to Project Directory
```bash
cd /Users/buddhisagarpanjiyar/Downloads/Yagesh
```

### Step 2: Build the Project
```bash
mvn clean install -DskipTests
```

**Expected output:** `BUILD SUCCESS`

### Step 3: Run the Application
```bash
mvn spring-boot:run
```

**Expected output:**
```
Intelligent Alert Escalation & Resolution System started successfully
```

### Step 4: Access the Application

Once running, access:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console
- **Health Check**: http://localhost:8080/actuator/health

## 📝 Configuration (Already Done)

The following are already configured and ready to use:

✅ **Database**: H2 in-memory (no setup needed)  
✅ **Security**: JWT authentication configured  
✅ **Default Users**: 
- Admin: `admin` / `admin123`
- Operator: `operator` / `operator123`  
✅ **Rules**: Pre-configured in `src/main/resources/rules.json`  
✅ **Caching**: Caffeine cache configured  
✅ **Monitoring**: Actuator endpoints enabled  

## 🔧 Optional Configurations

### For Production: Switch to PostgreSQL

1. **Install PostgreSQL:**
```bash
brew install postgresql@14
brew services start postgresql@14
```

2. **Create Database:**
```bash
createdb alertdb
```

3. **Update `application.yml`:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/alertdb
    username: your_username
    password: your_password
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

### Change Server Port (Optional)

If port 8080 is already in use, edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8081  # Change to any available port
```

### Update JWT Secret (Recommended for Production)

Edit `src/main/resources/application.yml`:

```yaml
jwt:
  secret: YOUR_VERY_LONG_SECRET_KEY_HERE_AT_LEAST_64_CHARACTERS_FOR_HS512
```

## 📦 What's Already Included (No Action Needed)

✅ All dependencies in `pom.xml`  
✅ Application configuration in `application.yml`  
✅ Rule definitions in `rules.json`  
✅ Default users auto-created on startup  
✅ Database schema auto-created (H2)  
✅ All source code ready to run  
✅ Comprehensive documentation  

## 🎯 Testing the System

### 1. Login to Get JWT Token

**Using cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "type": "Bearer",
    "username": "admin"
  }
}
```

**Copy the token value for next requests.**

### 2. Create an Alert

```bash
# Replace YOUR_TOKEN with the token from previous step
curl -X POST http://localhost:8080/api/v1/alerts \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "alertType": "OVERSPEEDING",
    "severity": "WARNING",
    "driverId": "D001",
    "vehicleId": "V001",
    "metadata": {
      "speed": 85,
      "limit": 60
    }
  }'
```

### 3. View Dashboard

```bash
curl -X GET http://localhost:8080/api/v1/dashboard/overview \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 🐛 Troubleshooting

### Issue: "Port 8080 already in use"
**Solution:** 
- Find and kill the process: `lsof -ti:8080 | xargs kill -9`
- Or change port in `application.yml`

### Issue: "Java version mismatch"
**Solution:**
```bash
# Set JAVA_HOME to Java 17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

### Issue: "Cannot connect to database"
**Solution:** 
- H2 runs in-memory, no connection needed
- Check logs for any errors

### Issue: "Build fails"
**Solution:**
```bash
# Clean and rebuild
mvn clean install -U -DskipTests

# If still fails, check Java version
java -version  # Should be 17+
mvn -version   # Should use Java 17
```

## 📚 Next Steps

After setup is complete:

1. ✅ **Read the documentation:**
   - `README.md` - Main documentation
   - `QUICKSTART.md` - Quick start guide
   - `ARCHITECTURE.md` - System architecture
   - `TRADEOFFS.md` - Design decisions

2. ✅ **Explore the API:**
   - Open Swagger UI: http://localhost:8080/swagger-ui.html
   - Try the demo scenarios in `QUICKSTART.md`

3. ✅ **Import Postman Collection:**
   - Import `postman_collection.json` into Postman
   - Collection includes all API endpoints

4. ✅ **Run Tests:**
   ```bash
   mvn test
   ```

## 🎉 System is Ready!

Once you complete the prerequisites:
- No additional setup needed
- No configuration files to create
- No database to configure (H2 works out of the box)
- Just build and run!

## 📞 Support

If you encounter any issues:
1. Check the logs in `logs/alert-system.log`
2. Review error messages in console
3. Check Actuator health: http://localhost:8080/actuator/health

---

**You're all set! Just install Java 17 and Maven, then run the project!** 🚀

