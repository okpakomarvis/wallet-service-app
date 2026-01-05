# ---------- BUILD STAGE ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests


# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy application JAR
COPY --from=build /build/target/*jar app.jar

# Copy entrypoint script
COPY entrypoint.sh /entrypoint.sh

EXPOSE 8080

# ENTRYPOINT reconstructs certs, CMD runs Java
ENTRYPOINT ["/entrypoint.sh"]
CMD ["java", "-jar", "app.jar"]
