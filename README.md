# Netflix Demo

A Spring Boot microservice-based Netflix-inspired media platform for managing movie metadata, uploads, transcoding, and streaming.

## Architecture

This repository contains four backend services:

- `content-service`: movie catalog and metadata management
- `video-service`: video upload orchestration and event publication
- `encoding-service`: video processing and FFmpeg-based transcode workflow
- `streaming-service`: presigned URL generation and HLS playlist serving

Supporting infrastructure:

- MySQL for catalog data
- Redis for caching signed streaming URLs
- Kafka for asynchronous event communication
- Docker Compose for local environment setup

## Services

### content-service
- Exposes movie catalog APIs
- Stores metadata in MySQL
- Provides search and retrieval endpoints for content

### video-service
- Receives uploaded video files
- Stores assets in AWS S3
- Publishes `VideoUploadedEvent` to Kafka

### encoding-service
- Consumes uploaded video events
- Runs FFmpeg encoding jobs
- Publishes `VideoEncodedEvent` when processing completes

### streaming-service
- Consumes encoding completion events
- Resolves S3 media content and builds signed URLs
- Provides playlist and stream access for clients

## Local infrastructure

The repository includes a Docker Compose setup for the shared dependencies:

```bash
docker compose up -d
```

This starts:

- Redis on `localhost:6379`
- MySQL on `localhost:3306`
- Zookeeper on default internal network
- Kafka on `localhost:9092`

## Prerequisites

- Java 17+
- Maven
- Docker and Docker Compose
- AWS credentials for S3 access
- FFmpeg installed and available on the system path

## Environment variables

Typical environment variables used by the services:

```bash
export AWS_ACCESS_KEY=your_access_key
export AWS_S3_SECRET_KEY=your_secret_key
export AWS_S3_REGION=us-east-1
export AWS_BUCKET_NAME=your-bucket
export AWS_S3_PRESIGNED_URL_EXPIRY=60
export FFMPEG_PATH=/usr/bin/ffmpeg
```

## Running services

From the repository root:

```bash
cd content-service
./mvnw spring-boot:run
```

```bash
cd video-service
./mvnw spring-boot:run
```

```bash
cd encoding-service
./mvnw spring-boot:run
```

```bash
cd streaming-service
./mvnw spring-boot:run
```

## Project structure

```text
Netflix/
├── content-service/
├── encoding-service/
├── streaming-service/
├── video-service/
├── docker-compose.yml
├── README.md
└── .gitignore
```

## Notes

- Some configuration files use environment-variable-backed defaults.
- Kafka and Redis are expected to be running before starting the application services.
- The project is designed for local/demo development and may require additional production hardening for real deployments.

## License

This project is intended for educational/demo use.
