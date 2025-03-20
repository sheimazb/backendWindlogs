@echo off
echo Starting Microservices...

echo Starting Config Server...
start cmd /k "cd config-server && mvn spring-boot:run"
timeout /t 20

echo Starting Discovery Service...
start cmd /k "cd discovery && mvn spring-boot:run"
timeout /t 20

echo Starting Authentication Service...
start cmd /k "cd authentication && mvn spring-boot:run"
timeout /t 20

echo Starting Tickets Service...
start cmd /k "cd tickets && mvn spring-boot:run"
timeout /t 20

echo Starting Notification Service...
start cmd /k "cd notification && mvn spring-boot:run"
timeout /t 20

echo Starting Gateway Service...
start cmd /k "cd gateway && mvn spring-boot:run"
timeout /t 20

echo All services started!
echo Access the Gateway at http://localhost:8222 