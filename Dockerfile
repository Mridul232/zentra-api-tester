# Use official OpenJDK image as base
FROM openjdk:8-jdk-alpine

# Set environment variables
ENV CATALINA_HOME /usr/local/tomcat
ENV PATH $CATALINA_HOME/bin:$PATH

# Install wget and unzip
RUN apk add --no-cache wget unzip

# Download and extract Apache Tomcat 8.5.87
RUN wget https://archive.apache.org/dist/tomcat/tomcat-8/v8.5.87/bin/apache-tomcat-8.5.87.tar.gz \
    && tar xzf apache-tomcat-8.5.87.tar.gz -C /usr/local/ \
    && mv /usr/local/apache-tomcat-8.5.87 $CATALINA_HOME \
    && rm apache-tomcat-8.5.87.tar.gz

# Copy backend WAR file to Tomcat webapps
COPY backend/target/api-tester.war $CATALINA_HOME/webapps/api-tester.war

# Copy frontend files to Tomcat webapps ROOT (optional)
COPY frontend/ $CATALINA_HOME/webapps/ROOT/

# Expose Tomcat port
EXPOSE 8080

# Health check (optional, for Render)
HEALTHCHECK CMD wget --spider -q http://localhost:8080/api-tester/ || exit 1

# Start Tomcat
CMD ["catalina.sh", "run"]
