# AI Document Intelligence Platform

A microservices-based system that analyzes documents using AI.

## Architecture

User → API Gateway → Auth Service → Document Service → AI Service → OpenAI

## Microservices

### API Gateway
Spring Cloud Gateway  
JWT validation

### Auth Service
User authentication  
JWT generation

### Document Service
Document upload  
Text extraction (Apache Tika)  
Async AI processing

### AI Service
OpenAI GPT integration  
Document summarization  
Classification and risk scoring

## Tech Stack

- Spring Boot
- Spring Cloud Gateway
- MongoDB
- Apache Tika
- OpenAI API
- Consul
- JWT Security
- Microservices Architecture

## Features

- Secure API Gateway authentication
- Asynchronous document processing
- AI-powered document summarization
- Document classification and risk analysis
- Microservice service discovery with Consul

## Future Improvements

- Vector embeddings for semantic search
- Docker containerization
- Kubernetes deployment