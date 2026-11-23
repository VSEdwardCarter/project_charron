
#!/usr/bin/env bash
set -Eeuo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BETA_DIR="$REPO_ROOT/infrastructure/beta"
DOCKERFILE="$REPO_ROOT/infrastructure/docker/api.Dockerfile"
SITE_URL="https://beta-api.vsfootage.com"

cd "$REPO_ROOT"

echo "========================================"
echo " CHARRON BETA DEPLOYMENT"
echo "========================================"

# Prevent overlapping deployments.
exec 9>/tmp/charron-beta-deploy.lock
flock -n 9 || {
    echo "Another deployment is running."
    exit 1
}

# Confirm we're on the expected server.
if [[ ! -f "$DOCKERFILE" ||
      ! -f "$BETA_DIR/compose.yaml" ||
      ! -f "$BETA_DIR/compose.caddy.yaml" ]]; then
    echo "ERROR: Lightsail deployment configuration not found."
    exit 1
fi

# Avoid overwriting local tracked changes.
if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "ERROR: Uncommitted tracked changes detected."
    exit 1
fi

# Verify sudo access without requesting a password.
sudo -n -v || {
    echo "ERROR: Passwordless sudo is required for deployment."
    exit 1
}

echo
echo "[1/7] Updating source"
git fetch origin main
git merge --ff-only origin/main

RELEASE="$(git rev-parse --short HEAD)"
echo "Release: $RELEASE"

echo
echo "[2/7] Building API"
sudo docker build \
    -f "$DOCKERFILE" \
    -t charron-api:beta-next \
    "$REPO_ROOT"

echo
echo "[3/7] Building frontend"

# Isolated build: no Node.js installation on Lightsail.
BUILD_DIR="$(mktemp -d)"
trap 'sudo rm -rf -- "$BUILD_DIR"' EXIT

sudo docker run --rm \
    -v "$REPO_ROOT/apps/web:/source:ro" \
    -v "$BUILD_DIR:/output" \
    -w /work \
    node:22-bookworm \
    sh -ec '
      tar -C /source \
        --exclude=node_modules \
        --exclude=dist \
        --exclude=.svelte-kit \
        -cf - . | tar -xf -

      npm ci
      npm run build
      cp -a dist/. /output/
    '

test -s "$BUILD_DIR/index.html"

echo
echo "[4/7] Publishing frontend"
rsync -a --delete \
    "$BUILD_DIR/" \
    "$BETA_DIR/web/"

echo
echo "[5/7] Activating API"

# Retain the previous API image for manual recovery.
if sudo docker image inspect charron-api:beta >/dev/null 2>&1; then
    sudo docker tag \
        charron-api:beta \
        charron-api:beta-previous
fi

sudo docker tag \
    charron-api:beta-next \
    charron-api:beta

DEPLOY_STARTED="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

cd "$BETA_DIR"

sudo docker compose \
    -f compose.yaml \
    -f compose.caddy.yaml \
    up -d --no-deps --force-recreate api

echo
echo "[6/7] Verifying services"

READY=false

for attempt in $(seq 1 40); do
    if sudo docker inspect charron-beta-api \
        --format '{{.State.Running}}' 2>/dev/null \
        | grep -qx true; then

        if sudo docker logs \
            --since "$DEPLOY_STARTED" \
            charron-beta-api 2>&1 \
            | grep -q "Started ApiApplication"; then

            READY=true
            break
        fi
    fi

    sleep 3
done

if [[ "$READY" != true ]]; then
    echo "ERROR: API did not complete startup."
    sudo docker logs --tail 100 charron-beta-api
    exit 1
fi

sudo docker exec charron-beta-postgres \
    sh -c 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"'

SCHEMA_VERSION="$(
    sudo docker exec charron-beta-postgres \
      sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At -c "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank DESC LIMIT 1;"'
)"

echo "Flyway schema version: $SCHEMA_VERSION"

echo
echo "[7/7] Checking HTTPS"
curl --fail --silent --show-error \
    --retry 5 \
    --retry-delay 2 \
    --output /dev/null \
    "$SITE_URL/"

echo
echo "========================================"
echo " CHARRON DEPLOYMENT SUCCESSFUL"
echo " Release: $RELEASE"
echo " Schema:  $SCHEMA_VERSION"
echo " URL:     $SITE_URL"
echo "========================================"
