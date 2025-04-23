# Utilise une image de base Java 17
FROM openjdk:17-jdk-alpine

# Argument pour spécifier dynamiquement le JAR à copier
ARG JAR_FILE=tickets-0.0.1.jar

# Port exposé défini dans application.yml
EXPOSE 8083

# Copie du fichier JAR dans l'image avec un nom standard
COPY ${JAR_FILE} app.jar

# Point d’entrée pour lancer l’application Spring Boot
ENTRYPOINT ["java", "-jar", "/app.jar"]
