# Build stage
FROM maven:3.8.7-openjdk-18-slim AS build

# create working directory for building
WORKDIR /build

# Copy everything from current context to the build folder
COPY pom.xml .
COPY AdapQuestBackend/pom.xml AdapQuestBackend/
COPY AdapQuestExchange/pom.xml AdapQuestExchange/
COPY AdapQuestExperiments/pom.xml AdapQuestExperiments/

RUN mvn clean package -Dmaven.main.skip -Dmaven.test.skip

COPY AdapQuestBackend/src AdapQuestBackend/src
COPY AdapQuestExchange/src AdapQuestExchange/src
COPY AdapQuestExperiments/src AdapQuestExperiments/src

# Build using maven
RUN mvn clean install package -Dmaven.test.skip

# Package stage
FROM openjdk:21-slim

# create app directory
WORKDIR /adaptive

# Copy executable fat-jar
COPY --from=build /build/AdapQuestBackend/target/adapquest-backend-*.jar adapquest-backend.jar
RUN mkdir -p "data/experiments" "data/results" "data/surveys" "data/templates"

# Expose service to port
EXPOSE 8080

# Execute command
CMD ["java", "-jar", "adapquest-backend.jar"]
