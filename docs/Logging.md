## Logging Setup

### Logback Configuration
The Logback configurations files (`logback.xml`) are located at:
```
project-minimization-plugin/src/main/resources/logback.xml
project-minimization-plugin/test/resources/logback.xml
```
Global Logger object with logger is located at
```
project-minimization-plugin/src/main/kotlin/.../plugin/logging/Loggers.kt
```


### Loggers and Their Outputs:

1. **General Logs** (root logger):
    - `INFO`, `WARN`, `ERROR` -> `logs/logs.log` and console (stdout)
    - `DEBUG`, `TRACE` -> `logs/logs.log`
   
    
    General logs used as usually for general purposes 


2. **Statistics Logs** (`STATISTICS` logger):
- **Level**: `INFO`, `DEBUG`
- **Output**: `logs/statistics.log`


    Statistical logs used for metrics collection

### Logger Usage Example:
```kotlin
private val generalLogger = KotlinLogging.logger {}

generalLogger.warn { "Log general warning" }
statLogger.info { "Log metric: 1" }
```

---