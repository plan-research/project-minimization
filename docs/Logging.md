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
   
    
    General logs used as usually in case smth can't be logged in Statistics or Working 


2. **Statistics Logs** (`STATISTICS` logger):
- **Level**: `INFO`, `DEBUG`
- **Output**: `logs/statistics.log`


    Statistical logs used for metrics collection

3. **Working Logs** (`WORKING` logger):
- **Level**: `INFO`
- **Output**: `logs/working.log`


    Working logs used for any useful information during plugin's work

### Logger Usage Example:
```kotlin
Logger.statLogger.info { "Logging a statistic" }
```

---