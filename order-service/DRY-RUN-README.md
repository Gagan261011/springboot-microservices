# Spring Boot Application Dry-Run Utility

## Overview

This utility provides a comprehensive dry-run diagnostic capability for Spring Boot microservices. It allows you to start and test your application configuration without connecting to external databases, APIs, or services, making it ideal for:

- Pre-deployment validation
- CI/CD pipeline health checks
- Development environment setup verification
- Configuration debugging
- Performance baseline measurements

## Features

✅ **Auto-detects** the main Spring Boot application class (annotated with `@SpringBootApplication`)  
✅ **Runs in isolated mode** - disables external connections (Eureka, Config Server, external databases)  
✅ **Comprehensive diagnostics** - displays environment variables, port bindings, and active profiles  
✅ **Bean analysis** - reports total beans loaded, categorized by package  
✅ **Graceful error handling** - captures and displays bean initialization failures in a readable format  
✅ **Performance metrics** - tracks startup time and context load time  
✅ **No side effects** - purely diagnostic, makes no modifications to your codebase  

## Requirements

- Java 21 (configured in build.gradle)
- Gradle 8.10.1 or later
- Spring Boot 3.3.3

## Usage

### Option 1: Using Gradle Task (Recommended)

```bash
# Set Java Home to Java 21
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64

# Run dry-run
./gradlew dryRun
```

### Option 2: Using Shell Script

The shell script provides a more user-friendly interface with colored output:

```bash
# Set Java Home to Java 21
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64

# Full dry-run (compile + start context)
./dry-run.sh

# Verify-only mode (compile + verify without starting context)
./dry-run.sh --verify-only

# Show help
./dry-run.sh --help
```

### Option 3: Direct Java Execution

```bash
# Build first
./gradlew build -x test

# Run the utility
java -cp build/libs/order-service-0.0.1-SNAPSHOT.jar \
  com.example.os.api.dryrun.DryRunUtility \
  com.example.os.api.OrderServiceApplication
```

## What Gets Disabled in Dry-Run Mode

The dry-run utility automatically disables external connections:

- **Eureka Service Discovery** - Sets `eureka.client.enabled=false`
- **Spring Cloud Config** - Sets `spring.cloud.config.enabled=false`
- **External Databases** - Uses in-memory H2 database (`jdbc:h2:mem:dryrun`)
- **Service Discovery** - Sets `spring.cloud.discovery.enabled=false`
- **DevTools Restart** - Disabled to prevent restart loops

## Output Format

The dry-run utility provides a comprehensive summary:

```
================================================================================
SPRING BOOT APPLICATION DRY-RUN UTILITY
================================================================================

✓ Main Application Class Detected: com.example.os.api.OrderServiceApplication
--------------------------------------------------------------------------------

================================================================================
DRY-RUN SUMMARY
================================================================================

📋 APPLICATION INFORMATION
--------------------------------------------------------------------------------
Application Name    : order-service
Application Version : 0.0.1-SNAPSHOT
Java Version        : 21.0.9
Java Vendor         : Eclipse Adoptium

🌍 ENVIRONMENT CONFIGURATION
--------------------------------------------------------------------------------
HOME                      : /home/runner
JAVA_HOME                 : /usr/lib/jvm/temurin-21-jdk-amd64
...

📝 KEY SPRING PROPERTIES
--------------------------------------------------------------------------------
spring.datasource.url               : jdbc:h2:mem:testdb
eureka.client.enabled               : false
spring.cloud.config.enabled         : false
...

🔌 PORT BINDINGS
--------------------------------------------------------------------------------
Server Port         : 9192
Management Port     : 9192
Context Path        : /

🏷️  ACTIVE PROFILES
--------------------------------------------------------------------------------
Active Profiles     : dryrun
  - dryrun

🫘 BEAN INFORMATION
--------------------------------------------------------------------------------
Total Beans Loaded  : 528

Beans by Category:
  Application Beans: 4
  Spring Framework Beans: 443
  Third-Party Beans: 81

Sample Application Beans:
  - orderServiceApplication
  - orderController
  - orderService
  ...

⏱️  TIMING INFORMATION
--------------------------------------------------------------------------------
Context Load Time   : 6469 ms
Total Startup Time  : 6757 ms
JVM Uptime          : 7170 ms

================================================================================
DRY-RUN COMPLETED SUCCESSFULLY
================================================================================
```

## Error Handling

If bean initialization fails, the utility provides detailed error information:

```
================================================================================
❌ DRY-RUN FAILED
================================================================================
Error Type: BeanCreationException
Error Message: Error creating bean with name 'orderService'...

Bean Initialization Failure Details:
--------------------------------------------------------------------------------
  [0] IllegalArgumentException: Could not resolve placeholder 'some.property'
  [1] PropertyPlaceholderException: Missing required property
  ...

Stack Trace (first 10 lines):
--------------------------------------------------------------------------------
  at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(...)
  at org.springframework.context.support.AbstractApplicationContext.refresh(...)
  ...
```

## Integration with CI/CD

### GitHub Actions Example

```yaml
name: Pre-Deployment Dry-Run

on:
  pull_request:
    branches: [ main ]

jobs:
  dry-run:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Run Dry-Run
        run: |
          cd order-service
          ./dry-run.sh
```

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any
    
    stages {
        stage('Dry-Run') {
            steps {
                script {
                    sh '''
                        cd order-service
                        export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
                        ./dry-run.sh
                    '''
                }
            }
        }
    }
}
```

## Customization

### Adding Custom Properties

To add custom properties for dry-run mode, edit `DryRunUtility.java`:

```java
// In the runDryRun method
dryRunProperties.put("your.custom.property", "value");
```

### Modifying Output

The utility is organized into separate print methods:
- `printApplicationInfo()` - Application metadata
- `printEnvironmentInfo()` - Environment variables and properties
- `printPortBindings()` - Port configurations
- `printActiveProfiles()` - Active Spring profiles
- `printBeanInfo()` - Bean statistics and samples
- `printStartupTime()` - Timing information

Modify these methods to customize the output.

## Troubleshooting

### Issue: ClassNotFoundException

**Cause:** Main application class not found  
**Solution:** Provide the fully qualified class name as an argument:

```bash
./gradlew dryRun -Dexec.args="com.example.os.api.OrderServiceApplication"
```

### Issue: UnsupportedClassVersionError

**Cause:** Running with incorrect Java version  
**Solution:** Ensure JAVA_HOME points to Java 21:

```bash
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
```

### Issue: Missing Property Placeholder

**Cause:** Required configuration property not provided in dry-run mode  
**Solution:** Add the property with a default value in `DryRunUtility.java`:

```java
dryRunProperties.put("missing.property.name", "default-value");
```

## Reusing for Other Services

The dry-run utility can be copied to other microservices:

1. Copy `DryRunUtility.java` to the target service's source directory
2. Update the package name in the file
3. Add the Gradle task to `build.gradle`:

```gradle
tasks.register('dryRun', JavaExec) {
    group = 'application'
    description = 'Runs the application in dry-run mode for diagnostics'
    classpath = sourceSets.main.runtimeClasspath
    mainClass = 'com.example.yourservice.dryrun.DryRunUtility'
    args = ['com.example.yourservice.YourServiceApplication']
    jvmArgs = [
        '-Dspring.profiles.active=dryrun',
        '-Dspring.cloud.config.enabled=false',
        '-Deureka.client.enabled=false'
    ]
}
```

4. Copy `dry-run.sh` and make it executable

## Best Practices

1. **Run before deployment** - Always run dry-run as part of your deployment pipeline
2. **Monitor startup time** - Track context load time to detect performance regressions
3. **Verify bean count** - Unexpected changes in bean count may indicate configuration issues
4. **Check active profiles** - Ensure the correct profiles are activated
5. **Review error messages** - Address any bean initialization failures before deployment

## Limitations

- Does not test actual runtime behavior with real external services
- Uses in-memory database instead of production database
- Cannot detect issues that only occur under load
- Does not validate network connectivity or firewall rules

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review the error output for specific failure details
3. Examine the Spring Boot logs for additional context

## License

This utility is part of the order-service microservice and follows the same license terms as the parent project.
