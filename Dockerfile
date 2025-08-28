# ---------- Build Stage ----------
FROM maven:3.8.8-eclipse-temurin AS build
WORKDIR /app
COPY backend /app/backend
COPY frontend /app/frontend
WORKDIR /app/backend
RUN mvn clean package

# ---------- Runtime Stage ----------
FROM openjdk:8-jdk-alpine
ENV CATALINA_HOME /usr/local/tomcat
ENV PATH $CATALINA_HOME/bin:$PATH
RUN apk add --no-cache wget unzip
RUN wget https://archive.apache.org/dist/tomcat/tomcat-8/v8.5.87/bin/apache-tomcat-8.5.87.tar.gz \
    && tar xzf apache-tomcat-8.5.87.tar.gz -C /usr/local/ \
    && mv /usr/local/apache-tomcat-8.5.87 $CATALINA_HOME \
    && rm apache-tomcat-8.5.87.tar.gz

# Copy built WAR from Maven image
COPY --from=build /app/backend/target/api-tester.war $CATALINA_HOME/webapps/api-tester.war
# Copy frontend files to Tomcat webapps ROOT (optional)
COPY frontend/ $CATALINA_HOME/webapps/ROOT/
EXPOSE 8080
HEALTHCHECK CMD wget --spider -q http://localhost:8080/api-tester/ || exit 1
CMD ["catalina.sh", "run"]
