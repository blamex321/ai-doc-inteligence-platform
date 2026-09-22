#!/bin/bash

# AI Document Intelligence Platform - Startup Script

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
mkdir -p "$ROOT_DIR/logs"
PID_FILE="$ROOT_DIR/.pids"

# Clear existing PID file
rm -f "$PID_FILE"

echo "=========================================================="
echo "🚀 Starting AI Document Intelligence Platform"
echo "=========================================================="

# 1. Verify Infrastructure
echo "🔍 Checking infrastructure..."

# Check Consul (Port 8500)
if ! nc -z localhost 8500 2>/dev/null; then
    echo "⚠️  Consul is not running on port 8500. Attempting to start via brew..."
    brew services start consul
    sleep 2
    if ! nc -z localhost 8500 2>/dev/null; then
        echo "❌ Failed to start Consul. Please run 'brew services start consul' or 'consul agent -dev'."
        exit 1
    fi
fi
echo "✅ Consul is running (http://localhost:8500/ui)"

# Check MongoDB (Port 27017)
if ! nc -z localhost 27017 2>/dev/null; then
    echo "⚠️  MongoDB is not running on port 27017. Attempting to start via brew..."
    brew services start mongodb-community
    sleep 3
    if ! nc -z localhost 27017 2>/dev/null; then
        echo "❌ Failed to start MongoDB. Please run 'brew services start mongodb-community'."
        exit 1
    fi
fi
echo "✅ MongoDB is running on port 27017"

# Check OpenAI API Key
if [ -z "$OPENAI_API_KEY" ]; then
    echo "⚠️  WARNING: OPENAI_API_KEY environment variable is NOT set!"
    echo "   ai-service may fail to start. Export it using: export OPENAI_API_KEY=\"your-key\""
fi

# Function to launch a service
start_service() {
    local SERVICE_NAME=$1
    local SERVICE_DIR="$ROOT_DIR/$SERVICE_NAME"
    local LOG_FILE="$ROOT_DIR/logs/$SERVICE_NAME.log"

    echo "▶️  Starting $SERVICE_NAME..."
    (cd "$SERVICE_DIR" && ./mvnw spring-boot:run > "$LOG_FILE" 2>&1) &
    local PID=$!
    echo "$SERVICE_NAME:$PID" >> "$PID_FILE"
}

# 2. Launch Services in Sequence
start_service "ai-service"
sleep 4
start_service "auth-service"
sleep 3
start_service "document-service"
sleep 3
start_service "api-gateway"

echo ""
echo "=========================================================="
echo "🎉 All services are launching in background!"
echo "=========================================================="
echo "Service Ports:"
echo "  - API Gateway:      http://localhost:8080 (Primary entry point)"
echo "  - Auth Service:     http://localhost:8081"
echo "  - Document Service: http://localhost:8082"
echo "  - AI Service:       http://localhost:8083"
echo "  - Consul Dashboard: http://localhost:8500/ui"
echo ""
echo "Logs are available in: $ROOT_DIR/logs/"
echo "To tail all logs:"
echo "  tail -f logs/*.log"
echo ""
echo "To stop all services:"
echo "  ./stop-all.sh"
echo "=========================================================="
