#!/bin/bash
set -euo pipefail

# ==============================
# 🎨 Colores
# ==============================
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; RED='\033[0;31m'; NC='\033[0m'

# ==============================
# 🧾 Paths
# ==============================
AUTH_DIR="modules/auth-service"
CONCILIATION_DIR="modules/conciliation-service"

AUTH_IMAGE="auth-service-app:latest"
CONCILIATION_IMAGE="conciliation-service-app:latest"

INFRA_DIR="infrastructure"
AUTH_INFRA="$INFRA_DIR/auth/auth-app.yaml"
AUTH_DB_INFRA="$INFRA_DIR/postgres/postgres-pod.yaml"
CONCILIATION_INFRA="$INFRA_DIR/conciliation/conciliation-app.yaml"
CONCILIATION_DB_INFRA="$INFRA_DIR/postgres/conciliation-postgres-pod.yaml"

# ==============================
# ⚙️ Funciones Utilitarias
# ==============================

log() { echo -e "${CYAN}$1${NC}"; }
info() { echo -e "${YELLOW}$1${NC}"; }
success() { echo -e "${GREEN}$1${NC}"; }
fail() { echo -e "${RED}$1${NC}"; }

wait_for_postgres() {
  local container_name="$1"
  local db="$2"
  info "⌛ Esperando base de datos '$db' en contenedor '$container_name'..."
  until podman exec "$container_name" psql -U admin -d "$db" -c '\q' > /dev/null 2>&1; do
    sleep 6
  done
  success "✅ Base de datos '$db' lista."
}

# ==============================
# 🛠️ Build y Deploy
# ==============================

log "📦 Compilando proyecto Maven..."
./mvnw clean package -DskipTests > /dev/null

[[ -f "$AUTH_DIR/target/auth-service-0.0.1-SNAPSHOT.jar" ]] || { fail "❌ auth-service JAR no encontrado"; exit 1; }
[[ -f "$CONCILIATION_DIR/target/conciliation-service-0.0.1-SNAPSHOT.jar" ]] || { fail "❌ conciliation-service JAR no encontrado"; exit 1; }
success "✅ JARs compilados correctamente."

log "🔨 Construyendo imágenes (en paralelo)..."
podman build -t "$AUTH_IMAGE" -f "$AUTH_DIR/Containerfile" "$AUTH_DIR" > /dev/null &
podman build -t "$CONCILIATION_IMAGE" -f "$CONCILIATION_DIR/Containerfile" "$CONCILIATION_DIR" > /dev/null &
wait
success "✅ Imágenes construidas."

log "🐘 Iniciando PostgreSQL para auth-service..."
podman play kube "$AUTH_DB_INFRA" > /dev/null
wait_for_postgres "auth-pod-auth-postgres" "authdb"

log "🚀 Iniciando auth-service..."
podman play kube "$AUTH_INFRA" > /dev/null
success "✅ auth-service desplegado."

log "🐘 Iniciando PostgreSQL para conciliation-service..."
podman play kube "$CONCILIATION_DB_INFRA" > /dev/null
wait_for_postgres "conciliation-postgres-pod-conciliation-postgres" "conciliationdb"

log "🚀 Iniciando conciliation-service..."
podman play kube "$CONCILIATION_INFRA" > /dev/null
success "✅ conciliation-service desplegado."

sleep 60
success "🎉 Todos los servicios están levantados correctamente."
