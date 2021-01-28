# Build stage
FROM maven:3.6.3-openjdk-11-slim AS build

# create working directory for building
WORKDIR /build

# Copy everything from current context to the build folder
COPY pom.xml .
COPY AdaptiveSurveyBackend/pom.xml AdaptiveSurveyBackend/
COPY AdaptiveSurveyExchange/pom.xml AdaptiveSurveyExchange/
COPY AdaptiveSurveyExperiments/pom.xml AdaptiveSurveyExperiments/

RUN mvn clean package -Dmaven.main.skip -Dmaven.test.skip

COPY AdaptiveSurveyBackend/src AdaptiveSurveyBackend/src
COPY AdaptiveSurveyExchange/src AdaptiveSurveyExchange/src
COPY AdaptiveSurveyExperiments/src AdaptiveSurveyExperiments/src

# Build using maven
RUN mvn clean install package -pl AdaptiveSurveyBackend -Dmaven.test.skip

RUN ls -l /build/AdaptiveSurveyBackend/target

# Package stage
FROM openjdk:11-jre-slim

# create app directory
WORKDIR /adaptive

# Copy executable fat-jar
COPY --from=build /build/AdaptiveSurveyBackend/target/adaptive-survey-backend-1.0-SNAPSHOT.jar adaptive-survey-backend.jar

# Expose service to port
EXPOSE 8080

# Execute command
CMD ["java", "-jar", "adaptive-survey-backend.jar"]