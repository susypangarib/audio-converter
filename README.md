
# Audio Converter Service

## Overview
The **Audio Converter Service** is a Java Spring Boot application that provides functionality to convert and manage audio files. The service allows users to upload M4A files, convert them to WAV format, store them in Google Cloud Storage (GCS), and retrieve them in different formats (M4A, MP3, WAV).
## Background
This project created to fulfill the requirements outline in this document https://docs.google.com/document/d/1DUlr2X4FVKt76MXtaY_O8LY5E8hYf8D2cauoO8lpTA0/edit?tab=t.0#heading=h.xil54aavm4ch
## Features
- Upload audio files in **M4A** format.
- Convert M4A to **WAV** before storage.
- Retrieve stored audio files in **WAV, M4A, or MP3** format.
- Store and fetch audio files from **Google Cloud Storage (GCS)**.

## Technologies Used
- **Java 17**
- **Spring Boot** (REST API, JPA, Validation)
- **FFmpeg** (for audio conversion)
- **Google Cloud Storage (GCS)** (for file storage)
- **PostgreSQL** (database for metadata storage)
- **JUnit 4 & Mockito** (for unit testing)

## Installation & Setup

### Prerequisites
- Java 17 or later
- Maven
- PostgreSQL Database
- Google Cloud Storage account & credentials
- FFmpeg installed on the system

### Clone the Repository
```sh
$ git clone https://github.com/susypangarib/audio-converter
$ cd audio-converter
```

### Configure Application Properties
Update `src/main/resources/application.properties`:
```properties
spring.application.name=Audio Converter
server.port=${PORT:8080}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.url=jdbc:postgresql://localhost:5432/audio-converter
spring.datasource.username=postgres
spring.datasource.password=postgrespw

spring.jpa.hibernate.ddl-auto=update

# Hikari connection pool properties
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.idle-timeout=30000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.pool-name=HikariPool

logging.level.org.springframework.web=DEBUG
logging.level.org.springframework.boot.autoconfigure.web=DEBUG

spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=100MB
spring.servlet.multipart.enabled=true

spring.mvc.throw-exception-if-no-handler-found=true
spring.web.resources.add-mappings=false

# GCP bucket properties
gcp.storage.bucket-name=audio_converter_thp
gcp.storage.folder-name=converted-audio
google.credentials=${GOOGLE_CREDENTIALS_JSON}
```

### Build & Run
```sh
$ mvn clean install
$ mvn spring-boot:run
```

## API Endpoints

### Upload Audio
```
POST http://localhost:8080/audio/user/{userId}/phrase/{phraseId}
```
**Request:**
- `userId` (String)
- `phraseId` (String)
- `file` (M4A file)

### Get Audio File
```
GET http://localhost:8080/audio/user/{userId}/phrase/{phraseId}/m4a
```
**Formats Supported:** `wav`, `m4a`, `mp3`

### Example Response
```HTTP/1.1 200 OK
Content-Type: audio/mpeg (or other format like audio/wav)
Content-Disposition: attachment; filename="audio.mp3"
Content-Length: <size>
```

## Unit Testing
To run the test suite:
```sh
$ mvn test
```

## License
This project is not licensed for public use, distribution, or modification. It is solely for personal use by the author.

---
### Contributors
- Susy Pangaribuan (pangaribuansusy@gmail.com)


### Preparing the Database

To set up the database for the Audio Converter application, follow the steps below:

#### 1. Create the Database
First, create a database named `audio-converter`. You can use the following SQL query to create the database:

```sql
CREATE DATABASE "audio-converter";
```
#### 2. Schema Creation
After creating the database, the schema will be automatically generated when you run the Spring Boot application. Ensure that your application is configured to connect to the audio-converter database. The schema will be created based on the entities defined in your Spring Boot application.

#### 3. Insert Master Data for Users
Once the schema is created, you need to insert the master data for users. Execute the following SQL query to insert the user data:
```sql 
INSERT INTO users (id, "name") VALUES
('4d9d6b40-e8f4-47ab-a10c-a9d3791ff2b1', 'User 1'),
('5d9d6b40-e8f4-47ab-a10c-a9d3791ff2b2', 'User 2'),
('6d9d6b40-e8f4-47ab-a10c-a9d3791ff2b3', 'User 3'),
('7d9d6b40-e8f4-47ab-a10c-a9d3791ff2b4', 'User 4'),
('8d9d6b40-e8f4-47ab-a10c-a9d3791ff2b5', 'User 5');
```
#### 4. Insert Master Data for Phrases
Next, insert the master data for phrases using the following SQL query:
```sql
INSERT INTO phrases (id, "name") VALUES
    ('4d9d6b40-e8f4-47ab-a10c-a9d3791ff2b9', 'Phrase 1'),
    ('5d9d6b40-e8f4-47ab-a10c-a9d3791ff2b9', 'Phrase 2'),
    ('6d9d6b40-e8f4-47ab-a10c-a9d3791ff2b9', 'Phrase 3'),
    ('7d9d6b40-e8f4-47ab-a10c-a9d3791ff2b9', 'Phrase 4'),
    ('8d9d6b40-e8f4-47ab-a10c-a9d3791ff2b9', 'Phrase 5');
```

#### 5. Database Ready
After completing the above steps, your database will be fully set up and ready to use with the Audio Converter application.


### Assumptions

The following assumptions have been made during the development of this project:

1. **Database Setup**:
    - The database is PostgreSQL, and the schema will be auto-generated by the Spring Boot application using Hibernate.
    - The database connection details (URL, username, password) are configured in the `application.properties` or `application.yml` file.

2. **User and Phrase Data**:
    - The `users` and `phrases` tables are pre-populated with master data as part of the database setup process.
    - UUIDs are used as primary keys for both `users` and `phrases` tables.
    - The `users` have one-to-many relationship with `audio` and `phrase` have one-to-one relationship with `audio`

3. **Audio Conversion**:
    - The application assumes that audio files are uploaded in a supported format (e.g., MP3, WAV) and will be converted to a target format specified by the user.
    - The conversion process is handled by a third-party library or service, and the application integrates with it seamlessly.

4. **File Storage**:
    - Uploaded audio files and converted files are stored in a predefined directory on the server or in cloud storage (e.g., AWS S3, Google Cloud Storage).
    - File paths or URLs are stored in the database for easy retrieval.

5. **Authentication and Authorization**:
    - The application assumes that user authentication and authorization are handled externally (e.g., via OAuth2, JWT, or a similar mechanism

### Running With Docker
Since this application uses Google Cloud Platform (GCP) and requires GOOGLE_CREDENTIALS_JSON, the service account key cannot be committed to GitHub for security reasons. Instead, you must add the key locally and provide it as an environment variable when running the Docker container.

Run the following command to start the application with Docker:
```docker run -p 8080:8080 \
-e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/audio-converter \
-e SPRING_DATASOURCE_USERNAME=postgres \
-e SPRING_DATASOURCE_PASSWORD=postgrespw \
-e SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect \
-e GOOGLE_APPLICATION_CREDENTIALS=/app/audio_converter_key.json \
audio-converter
```
Make sure that the Google Cloud credentials JSON file is present on your local machine and properly mapped inside the container.