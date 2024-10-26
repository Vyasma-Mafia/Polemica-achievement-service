# STAGE 1: Build the Kotlin app with Gradle
FROM gradle:jdk21 AS build

# Set the working directory in the container
WORKDIR /app

# Copy Gradle wrapper and build files
COPY build.gradle settings.gradle gradlew /app/

# Copy Gradle wrapper and required directories (if not using wrapper, you can install Gradle before this step)
COPY gradle /app/gradle

# Copy the rest of the source code
COPY src /app/src

# Run the Gradle build (this will use the Gradle wrapper inside the container)
RUN ./gradlew build

# STAGE 2: Create a minimal image to run the Kotlin app
FROM openjdk:21-jdk-slim

# Set the working directory for the new container
WORKDIR /app

# Copy the JAR file from the build stage into the final container
COPY --from=build /app/build/libs/*.jar /app/app.jar

# Expose the port that your app will run on (optional, change `8080` if needed)
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
