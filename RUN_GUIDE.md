# How to Run the AI Document Intelligence Platform

This guide outlines the exact, step-by-step instructions to run the entire platform in its current state on macOS.

---

## 1. System Architecture & Port Allocation

| Component | Port | Description | Database / External Dependency |
| :--- | :--- | :--- | :--- |
| **HashiCorp Consul** | `8500` | Service Discovery & Registry | Local agent (`http://localhost:8500/ui`) |
| **MongoDB** | `27017` | Persistent Database | Databases: `auth_db`, `document_db` |
| **OpenAI API** | Remote | LLM Provider (`gpt-4o-mini`) | `OPENAI_API_KEY` environment variable |
| **ai-service** | `8083` | Document Analysis & AI Summarization | OpenAI API |
| **auth-service** | `8081` | Authentication & JWT Generation | MongoDB (`mongodb://localhost:27017/auth_db`) |
| **document-service** | `8082` | File Upload, Text Extraction (Tika) | MongoDB (`mongodb://localhost:27017/document_db`) |
| **api-gateway** | `8080` | Client Entry Point, JWT Routing | Consul, Routes to `8081` & `8082` |

> [!NOTE]
> All client requests should go through the **API Gateway** on port **`8080`**.

---

## 2. Prerequisites & Infrastructure Setup

Before launching the Spring Boot microservices, ensure **MongoDB** and **Consul** are running.

### A. Start MongoDB
On macOS (via Homebrew):
```bash
brew services start mongodb-community
```
*(Verify it is running on port 27017: `nc -zv localhost 27017`)*

### B. Start HashiCorp Consul
If not already running via Homebrew:
```bash
brew services start consul
# Or run in foreground:
# consul agent -dev
```
Verify Consul is running by opening the web UI in your browser:
👉 **http://localhost:8500/ui**

### C. Set OpenAI API Key
The `ai-service` requires a valid OpenAI API key. Export it in your environment:
```bash
export OPENAI_API_KEY="your-openai-api-key-here"
```

---

## 3. Starting the Microservices

Open **4 separate terminal windows/tabs** (or use the automated startup script in Section 5).

### Terminal 1: AI Service (Port 8083)
```bash
cd "ai-service"
export OPENAI_API_KEY="your-openai-api-key-here"
./mvnw spring-boot:run
```
*Wait until you see:* `Started AiServiceApplication in X.XXX seconds`

---

### Terminal 2: Auth Service (Port 8081)
```bash
cd "auth-service"
./mvnw spring-boot:run
```
*Wait until you see:* `Started AuthServiceApplication in X.XXX seconds`

---

### Terminal 3: Document Service (Port 8082)
```bash
cd "document-service"
./mvnw spring-boot:run
```
*Wait until you see:* `Started DocumentServiceApplication in X.XXX seconds`

---

### Terminal 4: API Gateway (Port 8080)
```bash
cd "api-gateway"
./mvnw spring-boot:run
```
*Wait until you see:* `Started ApiGatewayApplication in X.XXX seconds`

---

## 4. End-to-End Verification & API Walkthrough

Once all 4 services and infrastructure are running, you can test the complete flow using `curl` (or Postman).

### Step 1: Register a New User
Create an account via the API Gateway:
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "Password123!"
  }'
```
**Expected Response:**
```json
{"message":"User registered successfully"}
```

---

### Step 2: Log In & Obtain JWT Token
Log in to generate your Bearer token:
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "Password123!"
  }'
```
**Expected Response:**
```json
{"token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."}
```

Save your token to an environment variable:
```bash
export TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

### Step 3: Test Gateway JWT Validation
Verify your token passes through the Gateway to the Auth Service:
```bash
curl -X GET http://localhost:8080/auth/whoami \
  -H "Authorization: Bearer $TOKEN"
```
**Expected Response:**
```
Authenticated as: testuser@example.com
```

---

### Step 4: Upload a Document for AI Processing
Upload a PDF, TXT, or DOCX file. In the current implementation, text extraction and OpenAI analysis are triggered on upload:
```bash
curl -X POST http://localhost:8080/documents/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/your/sample_document.pdf"
```
**Expected Response:**
```json
{
  "id": "6741b3e...",
  "fileName": "sample_document.pdf",
  "fileType": "application/pdf",
  "filePath": "172695..._sample_document.pdf",
  "uploadedBy": "testuser@example.com",
  "uploadedAt": "2026-09-23T02:00:00.000",
  "processingStatus": "COMPLETED",
  "summary": "...",
  "classification": "LLM-Generated",
  "riskScore": "Dynamic"
}
```

---

### Step 5: List Your Uploaded Documents
Retrieve all documents uploaded by your account:
```bash
curl -X GET http://localhost:8080/documents/my \
  -H "Authorization: Bearer $TOKEN"
```

---

### Step 6: Fetch Document AI Analysis
Check the analysis details for a specific document ID:
```bash
curl -X GET http://localhost:8080/documents/<DOCUMENT_ID>/analysis \
  -H "Authorization: Bearer $TOKEN"
```
**Expected Response:**
```json
{
  "status": "COMPLETED",
  "summary": "...",
  "classification": "LLM-Generated",
  "riskScore": "Dynamic"
}
```

---

### Step 7: Download the Document
Download the original uploaded document:
```bash
curl -X GET http://localhost:8080/documents/<DOCUMENT_ID>/download \
  -H "Authorization: Bearer $TOKEN" \
  --output downloaded_sample.pdf
```

---

## 5. Quick Startup Helper Scripts (Optional)

You can also use the helper scripts `start-all.sh` and `stop-all.sh` created in the root directory:

1. **Start all services in background**:
   ```bash
   export OPENAI_API_KEY="your-api-key"
   ./start-all.sh
   ```
2. **View live logs**:
   ```bash
   tail -f logs/*.log
   ```
3. **Stop all services**:
   ```bash
   ./stop-all.sh
   ```

---

## 6. Common Issues & Troubleshooting

| Issue | Root Cause | Solution |
| :--- | :--- | :--- |
| `ai-service` fails with `Could not resolve placeholder 'OPENAI_API_KEY'` | `OPENAI_API_KEY` environment variable is missing | Run `export OPENAI_API_KEY="sk-..."` before launching `ai-service`. |
| `auth-service` / `document-service` fail with Mongo connection errors | MongoDB daemon is not running | Run `brew services start mongodb-community` and verify port 27017. |
| Consul registration errors in logs | Consul agent is stopped | Run `brew services start consul` and check `http://localhost:8500`. |
| `HTTP 401 Unauthorized` through Gateway | Missing or malformed `Authorization` header | Ensure header is formatted as `Authorization: Bearer <TOKEN>`. |
| Port already in use (`Address already in use`) | A previous instance is still running | Find and kill PID: `lsof -i :<PORT>` then `kill -9 <PID>`. |
