# AI Document Intelligence Platform

A microservices-based document analysis platform that combines secure APIs, asynchronous document processing, and OpenAI-powered analysis.

## Architecture

```text
Client
  │
  ▼
API Gateway
  │
  ├──► Auth Service ──► JWT
  │
  └──► Document Service
           │
           ├──► Apache Tika (text extraction)
           │
           └──► Async processing
                    │
                    ▼
                AI Service
                    │
                    ▼
                 OpenAI
```

## Services

| Service | Responsibility |
|---|---|
| API Gateway | Entry point, routing, JWT validation |
| Auth Service | User authentication and JWT generation |
| Document Service | Uploads, text extraction, asynchronous processing |
| AI Service | Summarization, classification, and risk analysis |

## Key Features

- JWT-based authentication at the API gateway
- Consul-based service discovery
- Document upload and text extraction with Apache Tika
- Asynchronous document processing
- AI-powered document summarization
- Document classification and risk analysis
- MongoDB-backed persistence

## Tech Stack

**Backend:** Java, Spring Boot, Spring Cloud Gateway  
**Architecture:** Microservices, API Gateway, Service Discovery  
**Security:** JWT  
**Database:** MongoDB  
**Document Processing:** Apache Tika  
**AI:** OpenAI API  
**Service Discovery:** Consul

## Project Structure

```text
ai-doc-inteligence-platform/
├── api-gateway/
├── auth-service/
├── document-service/
├── ai-service/
└── README.md
```

## Configuration

Secrets are supplied through environment variables rather than committed to the repository.

```text
OPENAI_API_KEY=your-openai-api-key
```

## Getting Started

1. Install Java and Maven.
2. Start a local Consul instance.
3. Start MongoDB.
4. Configure the required environment variables.
5. Start the services individually, then access the platform through the API Gateway.

Refer to each service's configuration for its port and dependency settings.

## Future Improvements

- Vector embeddings and semantic search
- Docker Compose support
- Kubernetes deployment
- Automated tests and CI/CD
