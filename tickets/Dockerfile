# Utilise une image de base légère avec Java 17
FROM openjdk:17-jdk-alpine

# Argument pour spécifier le fichier JAR (fourni lors du build)
ARG JAR_FILE=backendwindlogs-1.0.0.jar

# Définit le port exposé par l'application
EXPOSE 8888

# Copie le JAR dans l'image avec un nom standard
COPY ${JAR_FILE} app.jar

# Commande pour lancer l'application
ENTRYPOINT ["java", "-jar", "/app.jar"]
