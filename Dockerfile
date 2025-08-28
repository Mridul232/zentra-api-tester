# ---------- Build Stage ----------
FROM maven:3.8.5-openjdk-17 AS build

# Set working directory to backend (where pom.xml exists)
WORKDIR /app/backend

# Copy only backend first (to cache dependencies)
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# Copy full backend source
COPY backend/ .

# Run Maven build (skip tests for speed)
RUN mvn clean package -DskipTests

# ---------- Runtime Stage ----------
FROM tomcat:9.0-jdk17

# Remove default ROOT app
RUN rm -rf /usr/local/tomcat/webapps/ROOT

# Copy backend WAR into Tomcat
COPY --from=build /app/backend/target/*.war /usr/local/tomcat/webapps/ROOT.war

# Copy frontend files (optional)
COPY frontend/ /usr/local/tomcat/webapps/ROOT/

# Expose Tomcat port
EXPOSE 8080

# Health check (Render)
HEALTHCHECK CMD curl -f http://localhost:8080/ || exit 1

# Start Tomcat
CMD ["catalina.sh", "run"]
