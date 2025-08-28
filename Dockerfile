# ---------- Stage 1: Build WAR with Maven ----------
FROM maven:3.8.5-openjdk-17 AS build

# Set working directory inside the container
WORKDIR /app

# Copy all project files into container
COPY . .

# Run Maven build (skip tests to speed up)
RUN mvn clean package -DskipTests


# ---------- Stage 2: Run on Tomcat ----------
FROM tomcat:9.0-jdk17

# Set environment variables
ENV CATALINA_HOME /usr/local/tomcat
ENV PATH $CATALINA_HOME/bin:$PATH

# Remove default ROOT app
RUN rm -rf $CATALINA_HOME/webapps/ROOT

# Copy the WAR built in Stage 1 into Tomcat as ROOT.war
COPY --from=build /app/backend/target/*.war $CATALINA_HOME/webapps/ROOT.war

# Copy frontend files into ROOT folder (optional: static hosting)
COPY frontend/ $CATALINA_HOME/webapps/ROOT/

# Expose Tomcat port
EXPOSE 8080

# Health check (optional, for Render)
HEALTHCHECK CMD curl --fail http://localhost:8080/ || exit 1

# Start Tomcat
CMD ["catalina.sh", "run"]
