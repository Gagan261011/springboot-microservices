package com.example.os.api.dryrun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dry-Run Utility for Spring Boot Applications
 * 
 * This utility performs a diagnostic dry-run of the Spring Boot application:
 * 1. Detects the main entry class with @SpringBootApplication
 * 2. Runs the app in dry-run mode without connecting to external DBs or APIs
 * 3. Prints environment variables, port bindings, and active profiles
 * 4. Lists bean initialization failures if any
 * 5. Generates a comprehensive dry-run summary
 */
public class DryRunUtility {

    private static final String SEPARATOR = "=".repeat(80);
    private static final String SUBSEPARATOR = "-".repeat(80);
    
    private static long startTime;
    private static long contextLoadTime;
    
    /**
     * Main entry point for dry-run execution
     * 
     * Usage: java -cp ... com.example.os.api.dryrun.DryRunUtility <MainApplicationClass>
     */
    public static void main(String[] args) {
        startTime = System.currentTimeMillis();
        
        printHeader();
        
        try {
            // Determine the main application class
            Class<?> mainClass = detectMainApplicationClass(args);
            
            if (mainClass == null) {
                System.err.println("ERROR: Could not detect main application class.");
                System.err.println("Usage: java -cp ... DryRunUtility [MainApplicationClassName]");
                System.exit(1);
            }
            
            System.out.println("✓ Main Application Class Detected: " + mainClass.getName());
            System.out.println(SUBSEPARATOR);
            
            // Run in dry-run mode
            ConfigurableApplicationContext context = runDryRun(mainClass);
            
            // Generate and print summary
            printSummary(context);
            
            // Close context
            context.close();
            
            System.out.println("\n" + SEPARATOR);
            System.out.println("DRY-RUN COMPLETED SUCCESSFULLY");
            System.out.println(SEPARATOR);
            
        } catch (Exception e) {
            // Ignore SilentExitException from DevTools
            if (e.getClass().getSimpleName().equals("SilentExitException")) {
                System.out.println("\nNote: DevTools restart triggered (ignored in dry-run mode)");
            } else {
                printError(e);
                System.exit(1);
            }
        }
    }
    
    /**
     * Detects the main application class with @SpringBootApplication annotation
     */
    private static Class<?> detectMainApplicationClass(String[] args) throws Exception {
        // If provided as argument
        if (args.length > 0) {
            try {
                return Class.forName(args[0]);
            } catch (ClassNotFoundException e) {
                System.err.println("Warning: Could not find class: " + args[0]);
            }
        }
        
        // Auto-detect from current package
        String currentPackage = DryRunUtility.class.getPackage().getName();
        String basePackage = currentPackage.substring(0, currentPackage.lastIndexOf('.'));
        
        // Try common patterns
        String[] possibleClasses = {
            basePackage + ".OrderServiceApplication",
            basePackage + ".Application",
            basePackage + ".Main"
        };
        
        for (String className : possibleClasses) {
            try {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(SpringBootApplication.class)) {
                    return clazz;
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        
        return null;
    }
    
    /**
     * Runs the application in dry-run mode with overridden configurations
     */
    private static ConfigurableApplicationContext runDryRun(Class<?> mainClass) {
        System.out.println("\nStarting application context in DRY-RUN mode...");
        System.out.println(SUBSEPARATOR);
        
        SpringApplication app = new SpringApplication(mainClass);
        
        // Add custom properties to disable external connections
        Map<String, Object> dryRunProperties = new HashMap<>();
        
        // Disable Eureka client
        dryRunProperties.put("eureka.client.enabled", "false");
        dryRunProperties.put("eureka.client.register-with-eureka", "false");
        dryRunProperties.put("eureka.client.fetch-registry", "false");
        
        // Disable Cloud Config
        dryRunProperties.put("spring.cloud.config.enabled", "false");
        dryRunProperties.put("spring.cloud.config.import-check.enabled", "false");
        
        // Use in-memory H2 database
        dryRunProperties.put("spring.datasource.url", "jdbc:h2:mem:dryrun");
        dryRunProperties.put("spring.jpa.hibernate.ddl-auto", "create-drop");
        
        // Disable external service calls
        dryRunProperties.put("spring.cloud.discovery.enabled", "false");
        
        // Set dry-run profile
        dryRunProperties.put("spring.profiles.active", "dryrun");
        
        // Provide default values for external service endpoints
        dryRunProperties.put("microservices.payment-service.endpoints.endpoint.url", "http://localhost:9191/payment/doPayment");
        
        // Disable DevTools to prevent restart issues
        dryRunProperties.put("spring.devtools.restart.enabled", "false");
        
        app.setDefaultProperties(dryRunProperties);
        
        // Add context refresh listener
        app.addListeners((ApplicationListener<ContextRefreshedEvent>) event -> {
            contextLoadTime = System.currentTimeMillis() - startTime;
            System.out.println("✓ Application context loaded successfully in " + contextLoadTime + "ms");
        });
        
        return app.run();
    }
    
    /**
     * Prints comprehensive dry-run summary
     */
    private static void printSummary(ApplicationContext context) {
        System.out.println("\n" + SEPARATOR);
        System.out.println("DRY-RUN SUMMARY");
        System.out.println(SEPARATOR);
        
        Environment env = context.getEnvironment();
        
        // Application Info
        printApplicationInfo(env);
        
        // Environment Variables and System Properties
        printEnvironmentInfo(env);
        
        // Port Bindings
        printPortBindings(env);
        
        // Active Profiles
        printActiveProfiles(env);
        
        // Bean Information
        printBeanInfo(context);
        
        // Startup Time
        printStartupTime();
    }
    
    private static void printApplicationInfo(Environment env) {
        System.out.println("\n📋 APPLICATION INFORMATION");
        System.out.println(SUBSEPARATOR);
        System.out.println("Application Name    : " + env.getProperty("spring.application.name", "N/A"));
        System.out.println("Application Version : " + env.getProperty("application.version", "0.0.1-SNAPSHOT"));
        System.out.println("Spring Boot Version : " + env.getProperty("spring.boot.version", "N/A"));
        System.out.println("Java Version        : " + System.getProperty("java.version"));
        System.out.println("Java Vendor         : " + System.getProperty("java.vendor"));
    }
    
    private static void printEnvironmentInfo(Environment env) {
        System.out.println("\n🌍 ENVIRONMENT CONFIGURATION");
        System.out.println(SUBSEPARATOR);
        
        // Key environment variables
        Map<String, String> keyEnvVars = new TreeMap<>();
        String[] importantVars = {"JAVA_HOME", "PATH", "USER", "HOME", "SPRING_PROFILES_ACTIVE"};
        
        for (String var : importantVars) {
            String value = System.getenv(var);
            if (value != null) {
                // Truncate long values
                if (value.length() > 80) {
                    value = value.substring(0, 77) + "...";
                }
                keyEnvVars.put(var, value);
            }
        }
        
        keyEnvVars.forEach((key, value) -> 
            System.out.println(String.format("%-25s : %s", key, value))
        );
        
        // Important Spring properties
        System.out.println("\n📝 KEY SPRING PROPERTIES");
        System.out.println(SUBSEPARATOR);
        String[] keyProps = {
            "spring.datasource.url",
            "spring.jpa.hibernate.ddl-auto",
            "eureka.client.enabled",
            "spring.cloud.config.enabled",
            "logging.level.root"
        };
        
        for (String prop : keyProps) {
            String value = env.getProperty(prop);
            if (value != null) {
                System.out.println(String.format("%-35s : %s", prop, value));
            }
        }
    }
    
    private static void printPortBindings(Environment env) {
        System.out.println("\n🔌 PORT BINDINGS");
        System.out.println(SUBSEPARATOR);
        String serverPort = env.getProperty("server.port", "8080");
        String managementPort = env.getProperty("management.server.port", serverPort);
        
        System.out.println("Server Port         : " + serverPort);
        System.out.println("Management Port     : " + managementPort);
        System.out.println("Context Path        : " + env.getProperty("server.servlet.context-path", "/"));
    }
    
    private static void printActiveProfiles(Environment env) {
        System.out.println("\n🏷️  ACTIVE PROFILES");
        System.out.println(SUBSEPARATOR);
        String[] profiles = env.getActiveProfiles();
        if (profiles.length == 0) {
            profiles = env.getDefaultProfiles();
            System.out.println("Active Profiles     : (using default)");
        } else {
            System.out.println("Active Profiles     : " + String.join(", ", profiles));
        }
        
        for (String profile : profiles) {
            System.out.println("  - " + profile);
        }
    }
    
    private static void printBeanInfo(ApplicationContext context) {
        System.out.println("\n🫘 BEAN INFORMATION");
        System.out.println(SUBSEPARATOR);
        
        String[] allBeans = context.getBeanDefinitionNames();
        System.out.println("Total Beans Loaded  : " + allBeans.length);
        
        // Categorize beans
        Map<String, List<String>> beansByPackage = Arrays.stream(allBeans)
            .filter(name -> {
                try {
                    Object bean = context.getBean(name);
                    return bean != null;
                } catch (Exception e) {
                    return false;
                }
            })
            .collect(Collectors.groupingBy(name -> {
                try {
                    Object bean = context.getBean(name);
                    String packageName = bean.getClass().getPackage() != null ? 
                        bean.getClass().getPackage().getName() : "default";
                    
                    // Group by base package
                    if (packageName.startsWith("com.example")) {
                        return "Application Beans";
                    } else if (packageName.startsWith("org.springframework")) {
                        return "Spring Framework Beans";
                    } else {
                        return "Third-Party Beans";
                    }
                } catch (Exception e) {
                    return "Other";
                }
            }));
        
        System.out.println("\nBeans by Category:");
        beansByPackage.forEach((category, beans) -> {
            System.out.println("  " + category + ": " + beans.size());
        });
        
        // Show sample application beans
        List<String> appBeans = beansByPackage.getOrDefault("Application Beans", new ArrayList<>());
        if (!appBeans.isEmpty()) {
            System.out.println("\nSample Application Beans:");
            appBeans.stream()
                .limit(10)
                .forEach(bean -> System.out.println("  - " + bean));
            if (appBeans.size() > 10) {
                System.out.println("  ... and " + (appBeans.size() - 10) + " more");
            }
        }
    }
    
    private static void printStartupTime() {
        System.out.println("\n⏱️  TIMING INFORMATION");
        System.out.println(SUBSEPARATOR);
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Context Load Time   : " + contextLoadTime + " ms");
        System.out.println("Total Startup Time  : " + totalTime + " ms");
        
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        System.out.println("JVM Uptime          : " + runtimeMXBean.getUptime() + " ms");
    }
    
    private static void printHeader() {
        System.out.println("\n" + SEPARATOR);
        System.out.println("SPRING BOOT APPLICATION DRY-RUN UTILITY");
        System.out.println(SEPARATOR);
        System.out.println("This utility performs a diagnostic dry-run without external connections");
        System.out.println(SEPARATOR + "\n");
    }
    
    private static void printError(Exception e) {
        System.err.println("\n" + SEPARATOR);
        System.err.println("❌ DRY-RUN FAILED");
        System.err.println(SEPARATOR);
        System.err.println("Error Type: " + e.getClass().getSimpleName());
        System.err.println("Error Message: " + e.getMessage());
        System.err.println("\nBean Initialization Failure Details:");
        System.err.println(SUBSEPARATOR);
        
        Throwable cause = e.getCause();
        int depth = 0;
        while (cause != null && depth < 5) {
            System.err.println("  [" + depth + "] " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
            cause = cause.getCause();
            depth++;
        }
        
        System.err.println("\nStack Trace (first 10 lines):");
        System.err.println(SUBSEPARATOR);
        StackTraceElement[] stackTrace = e.getStackTrace();
        for (int i = 0; i < Math.min(10, stackTrace.length); i++) {
            System.err.println("  at " + stackTrace[i]);
        }
        
        if (stackTrace.length > 10) {
            System.err.println("  ... " + (stackTrace.length - 10) + " more");
        }
    }
}
