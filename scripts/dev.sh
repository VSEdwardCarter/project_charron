
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
API_DIR="$ROOT_DIR/apps/api"
WEB_DIR="$ROOT_DIR/apps/web"
ENV_FILE="$ROOT_DIR/infrastructure/.env"

api_pid=""
web_pid=""

cleanup() {
    echo
    echo "Stopping CHARRON application servers..."

    if [[ -n "$web_pid" ]]; then
        kill -- "-$web_pid" 2>/dev/null || true
    fi

    if [[ -n "$api_pid" ]]; then
        kill -- "-$api_pid" 2>/dev/null || true
    fi

    wait 2>/dev/null || true

    echo "Application servers stopped."
    echo "PostgreSQL remains running."
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

echo "===== CHARRON DEVELOPMENT ENVIRONMENT ====="

# Load database configuration.
set -a
source "$ENV_FILE"
set +a

# Load Node.js through NVM.
export NVM_DIR="${NVM_DIR:-$HOME/.nvm}"
source "$NVM_DIR/nvm.sh"
nvm use v22.21.1 >/dev/null

# Start existing PostgreSQL container if necessary.
echo "Checking PostgreSQL..."

if ! docker container inspect charron-postgres >/dev/null 2>&1; then
    echo "Missing Docker container: charron-postgres"
    echo "Initialize PostgreSQL using the infrastructure setup."
    exit 1
fi

if [[ "$(docker inspect -f '{{.State.Running}}' charron-postgres)" != "true" ]]; then
    docker start charron-postgres
fi

echo "PostgreSQL is running."

# Start Spring Boot in its own process group.
echo "Starting Spring Boot..."

setsid bash -c '
    cd "$1"
    exec ./gradlew bootRun
' _ "$API_DIR" &

api_pid=$!

# Start Vite in its own process group.
echo "Starting Svelte frontend..."

setsid bash -c '
    cd "$1"
    exec npm run dev -- --strictPort
' _ "$WEB_DIR" &

web_pid=$!

echo
echo "===== CHARRON SERVICES ====="
echo "Frontend: http://localhost:5173"
echo "API:      http://localhost:8080/api/equipment"
echo "Swagger:  http://localhost:8080/swagger-ui.html"
echo
echo "Press Ctrl+C to stop CHARRON."
echo

# Exit if either application server exits.
wait -n "$api_pid" "$web_pid"
