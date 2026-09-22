# 📄 AI Document Intelligence & Analytics Platform

[![Java](https://img.shields.io/badge/Java-20%2F17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-Gateway%20%7C%20Consul-blue.svg)](https://spring.io/projects/spring-cloud)
[![MongoDB](https://img.shields.io/badge/MongoDB-NoSQL-green.svg)](https://www.mongodb.com/)
[![OpenAI](https://img.shields.io/badge/OpenAI-GPT--4o--mini-purple.svg)](https://openai.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://www.docker.com/)

A production-grade, distributed microservices platform designed to ingest, extract, classify, and analyze enterprise documents (PDF, DOCX, TXT) asynchronously using Apache Tika and OpenAI LLMs.

---

## 🏗️ Architecture Overview

```
                          ┌─────────────────────────────┐
                          │   Client / Frontend App     │
                          └──────────────┬──────────────┘
                                         │ (Bearer JWT / Multipart)
                                         ▼
                     ┌───────────────────────────────────────┐
                     │    API Gateway (Spring Cloud, 8080)   │
                     │  - JWT Auth Filter & Spoofing Guard   │
                     │  - Dynamic Service Routing (Consul)   │
                     └───────┬───────────────────────┬───────┘
                             │                       │
           /auth/** Requests │                       │ /documents/** Requests
                             ▼                       ▼
       ┌───────────────────────────┐   ┌───────────────────────────────┐
       │ Auth Service (Port 8081)  │   │ Document Service (Port 8082)  │
       │ - User Registration       │   │ - File Ingestion (Anti-Path)  │
       │ - BCrypt & Stateless JWT  │   │ - Text Extraction (Tika)      │
       │ - RBAC Claims (USER/ADMIN)│   │ - Async Pipeline Execution    │
       └─────────────┬─────────────┘   └───────────────┬───────────────┘
                     │                                 │
                     ▼ (auth_db)                       ▼ (document_db + local/volume storage)
              [( MongoDB )]                     [( MongoDB )]
                                                       │
                                                       │ Async HTTP Call
                                                       ▼
                                       ┌───────────────────────────────┐
                                       │    AI Service (Port 8083)     │
                                       │ - Structured JSON Inference   │
                                       │ - Heuristic Fallback Engine   │
                                       │ - Head-Tail Token Truncation  │
                                       └───────────────┬───────────────┘
                                                       │
                                                       ▼
                                            [ OpenAI GPT-4o-mini ]
```

---

## 🌟 Key Technical Highlights & Enhancements

1. **True Asynchronous Pipeline (`@Async` Proxy Architecture)**:
   - Eliminated self-invocation Spring AOP proxy bypass bugs by isolating document processing in a dedicated `AsyncDocumentProcessor` bean. Upload HTTP requests return immediately with `PROCESSING` status while text extraction and LLM inference proceed in background threads.
2. **Structured LLM Inference & Resilience**:
   - Instructs OpenAI with strict JSON schemas (`response_format: { type: "json_object" }`).
   - Automatically parses executive summaries, document classifications (e.g., `Resume`, `Financial`, `Legal`, `Technical`), risk scores (`Low`, `Medium`, `High`), and key concepts.
   - Built-in heuristic fallback mode ensures uninterrupted operation even without active OpenAI API keys during development.
3. **Enterprise Security & Perimeter Defense**:
   - **Header Anti-Spoofing**: Gateway automatically strips untrusted `X-User-Email` and `X-User-Role` headers before verifying JWT tokens and injecting authenticated claims.
   - **Path Traversal Protection**: Uploaded filenames are sanitized (`StringUtils.cleanPath`) to defend against directory traversal exploits (`../../`).
   - **Ownership Isolation**: Document access, analysis, and download endpoints enforce object-level ownership checks (BOLA protection).
4. **Memory-Safe File Streaming**:
   - Replaced full-memory `byte[]` reading with streaming `FileSystemResource` wrapped in `ResponseEntity<Resource>` with MIME detection and content disposition.
5. **Clean Separation of Concerns**:
   - Completely decoupled Controller and Persistence layers by eliminating direct Repository calls from REST controllers.
   - Enforced typed DTO contracts (`DocumentResponse`, `DocumentAnalysisResponse`, `PagedResponse`, `ErrorResponse`).
6. **Document RAG & Conversational Q&A ("Chat with Document")**:
   - Semantic sliding-window chunking engine splits ingested texts into overlapping context segments.
   - Relevance retrieval ranks chunks against user queries and feeds grounded context to `gpt-4o-mini` with strict anti-hallucination instructions.
   - Transparent source citations drawer surfaces the exact retrieved document excerpts alongside AI responses.
7. **Interactive Modern React Dashboard (`frontend/`)**:
   - Built with React 19, Vite, and Tailwind CSS.
   - Features real-time JWT authentication, drag-and-drop document upload, analytics overview metrics, instant PDF/DOCX downloads, and a conversational RAG chat modal with suggested query chips.
8. **Containerization & Automated Testing**:
   - Multi-stage Dockerfiles for all microservices and frontend with unified `docker-compose.yml`.
   - Comprehensive unit test suites utilizing **JUnit 5**, **Mockito**, and MockMvc across all services.

---

## 📦 Microservices Breakdown

| Service | Port | Primary Responsibilities | Dependencies |
| :--- | :--- | :--- | :--- |
| **`api-gateway`** | `8080` | Reverse proxy, JWT validation, header sanitization, load balancing | Spring Cloud Gateway, Consul |
| **`auth-service`** | `8081` | User registration, password encryption (BCrypt), JWT generation | MongoDB, Spring Security |
| **`document-service`**| `8082` | Multipart upload, Apache Tika parsing, async processing, file streaming | MongoDB, Apache Tika |
| **`ai-service`** | `8083` | OpenAI completions, JSON schema enforcement, token budget management | WebFlux, Jackson, OpenAI |

---

## 🚀 Quick Start Guide

### Option 1: Docker Compose (All-in-One)

```bash
# Set your OpenAI API key
export OPENAI_API_KEY="your-api-key-here"

# Boot the entire infrastructure + all 4 services
docker compose up --build
```
*Access API Gateway at: `http://localhost:8080`*  
*Access Consul Dashboard at: `http://localhost:8500/ui`*

---

### Option 2: Local Development (Native)

#### 1. Start Infrastructure
```bash
brew services start mongodb-community
brew services start consul
```

#### 2. Start Services via Helper Scripts
```bash
export OPENAI_API_KEY="your-api-key-here"
./start-all.sh
```
*To stop: `./stop-all.sh`*  
*To view logs: `tail -f logs/*.log`*

For complete manual step-by-step instructions, see [`RUN_GUIDE.md`](./RUN_GUIDE.md).

---

## 📡 API Reference & Verification

All requests are routed through **API Gateway** (`http://localhost:8080`).

### 1. Authentication
* **Register**:
  ```bash
  curl -X POST http://localhost:8080/auth/register \
    -H "Content-Type: application/json" \
    -d '{"email":"dev@company.com","password":"Password123!"}'
  ```
* **Login**:
  ```bash
  curl -X POST http://localhost:8080/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"dev@company.com","password":"Password123!"}'
  ```
  *Copy the returned token to `$TOKEN`: `export TOKEN="<jwt-token>"`*

---

### 2. Document Operations
* **Upload Document**:
  ```bash
  curl -X POST http://localhost:8080/documents/upload \
    -H "Authorization: Bearer $TOKEN" \
    -F "file=@/path/to/invoice.pdf"
  ```
* **List Uploaded Documents (Paginated)**:
  ```bash
  curl -X GET "http://localhost:8080/documents/my?page=0&size=10&sortBy=uploadedAt&sortDir=desc" \
    -H "Authorization: Bearer $TOKEN"
  ```
* **Get AI Analysis**:
  ```bash
  curl -X GET http://localhost:8080/documents/<DOCUMENT_ID>/analysis \
    -H "Authorization: Bearer $TOKEN"
  ```
* **Document RAG & Conversational Q&A**:
  ```bash
  curl -X POST http://localhost:8080/documents/<DOCUMENT_ID>/chat \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"question":"What are the payment terms and due dates?"}'
  ```
* **Download Document**:
  ```bash
  curl -X GET http://localhost:8080/documents/<DOCUMENT_ID>/download \
    -H "Authorization: Bearer $TOKEN" \
    --output downloaded_doc.pdf
  ```
* **Delete Document**:
  ```bash
  curl -X DELETE http://localhost:8080/documents/<DOCUMENT_ID> \
    -H "Authorization: Bearer $TOKEN"
  ```

---

## 🧪 Running Automated Tests

Run the test suite across the microservices:

```bash
# Auth Service Tests (5 tests)
cd auth-service && ./mvnw test && cd ..

# Document Service Tests (8 tests)
cd document-service && ./mvnw test && cd ..

# AI Service Tests (5 tests)
cd ai-service && ./mvnw test && cd ..
```

---

## 📋 Technology Stack

* **Language & Runtime**: Java 20 / 17, Spring Boot 3.x
* **Gateway & Service Mesh**: Spring Cloud Gateway, HashiCorp Consul, Spring Cloud LoadBalancer
* **Security**: Spring Security, JJWT, BCrypt, Stateless RBAC
* **Data & Storage**: MongoDB (Spring Data MongoDB), Local Volume / FileSystem
* **Text Extraction**: Apache Tika 2.9 (PDF, Word, Excel, Plain Text)
* **AI & LLM Integration**: OpenAI API (`gpt-4o-mini`), Jackson JSON Schema Parser
* **DevOps**: Docker, Docker Compose, Maven Multi-Stage Builds
* **Testing**: JUnit 5, Mockito, Spring Test