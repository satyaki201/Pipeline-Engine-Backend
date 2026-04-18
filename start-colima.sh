#!/bin/bash

set -e

echo "Checking Colima status..."
if ! colima status &>/dev/null; then
    echo "Colima is not running. Starting Colima..."
    colima start
    # Wait a moment for Colima to fully initialize
    sleep 3
else
    echo "Colima is already running"
fi

# Ensure the Docker socket is accessible
COLIMA_SOCK="${HOME}/.colima/default/docker.sock"
if [ ! -S "$COLIMA_SOCK" ]; then
    echo "Error: Colima Docker socket not found at $COLIMA_SOCK"
    echo "Please ensure Colima started successfully with: colima status"
    exit 1
fi

echo "✓ Colima socket verified at: $COLIMA_SOCK"

# Start docker-compose
echo "Starting docker-compose services..."
docker-compose up -d

# Wait for services to be ready
echo "Waiting for services to be ready..."
sleep 5

echo ""
echo "✓ Services started successfully. You can access:"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo "  - Alloy UI: http://localhost:12345"
echo "  - Loki: http://localhost:3100"
echo ""
echo "To verify Alloy can discover containers:"
echo "  1. Open http://localhost:12345"
echo "  2. Go to Targets → discovery.docker.containers"
echo "  3. You should see running containers listed"


