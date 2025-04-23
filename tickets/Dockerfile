# Utilise une image OpenJDK comme base
FROM openjdk:17-jdk-alpine

# Définir un argument pour le fichier JAR (qui sera passé via la commande `docker build`)
ARG JAR_FILE

# Crée un répertoire pour l'application
WORKDIR /app

# Copie le fichier JAR dans l'image, en utilisant le nom d'argument
COPY ${JAR_FILE} app.jar

# Expose le port sur lequel l'application Spring Boot va écouter
EXPOSE 8083

# Commande pour démarrer l'application Spring Boot
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
