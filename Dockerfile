# Use OpenJDK as base image
FROM openjdk:17-jdk-slim

# Install FFmpeg
RUN apt-get update && apt-get install -y ffmpeg
RUN ln -s /usr/bin/ffmpeg /usr/local/bin/ffmpeg

# Set environment variables
ENV APP_HOME=/app
ENV GOOGLE_APPLICATION_CREDENTIALS=$APP_HOME/audio_converter_key.json

# Create application directory
WORKDIR $APP_HOME

# Install FFmpeg
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    apt-get clean

# Copy application JAR file
COPY target/converter-0.0.1-SNAPSHOT.jar app.jar

# Copy Google Cloud credentials (ensure it's mounted via Docker secrets or env variables)
COPY audio_converter_key.json audio_converter_key.json

# Expose application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
