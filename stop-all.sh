#!/bin/bash

# AI Document Intelligence Platform - Shutdown Script

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$ROOT_DIR/.pids"

echo "=========================================================="
echo "🛑 Stopping AI Document Intelligence Platform Services"
echo "=========================================================="

if [ -f "$PID_FILE" ]; then
    while IFS=":" read -r SERVICE_NAME PID; do
        if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
            echo "Stopping $SERVICE_NAME (PID $PID)..."
            kill "$PID" 2>/dev/null
        fi
    done < "$PID_FILE"
    rm -f "$PID_FILE"
fi

# Kill any remaining instances by port to ensure clean ports
PORTS=(3000 8080 8081 8082 8083)
for PORT in "${PORTS[@]}"; do
    PID=$(lsof -ti :"$PORT" 2>/dev/null)
    if [ -n "$PID" ]; then
        echo "Clearing port $PORT (PID $PID)..."
        kill -9 "$PID" 2>/dev/null
    fi
done

echo "✅ All microservices and frontend stopped."
echo "=========================================================="
