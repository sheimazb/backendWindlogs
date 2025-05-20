# Utilise une image OpenJDK comme base
FROM openjdk:17-jdk-alpine

# Définir un argument pour le fichier JAR (qui sera passé via la commande `docker build`)
ARG JAR_FILE

# Copie dynamique du fichier JAR (passé en build arg)
ADD ${JAR_FILE} app.jar

# Expose le port sur lequel l'application Spring Boot va écouter
EXPOSE 8888

# Commande pour démarrer l'application Spring Boot
ENTRYPOINT ["java", "-jar", "/app.jar"] 