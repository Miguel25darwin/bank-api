# ================================================================
#  Bank API — Dockerfile
#  Image multi-stage : build Maven → runtime JRE allégé
# ================================================================

# ---- Stage 1 : Build ----
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
# Pré-téléchargement des dépendances (couche Docker cachée si pom.xml inchangé)
RUN mvn dependency:go-offline --no-transfer-progress
COPY src ./src
RUN mvn package -DskipTests --no-transfer-progress

# ---- Stage 2 : Runtime ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Copier uniquement le JAR final
COPY --from=builder /app/target/bank-api-1.0.0.jar app.jar

# Port exposé (doit correspondre à server.port ou PORT dans Render)
EXPOSE 8080

# Point d'entrée — active le profil prod
ENTRYPOINT ["java", \
            "-Dspring.profiles.active=prod", \
            "-Dserver.port=${PORT:-8080}", \
            "-jar", "app.jar"]
