#!/bin/bash

set -euo pipefail

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

# Paths y nombres comunes
AUTH_SERVICE_DIR="modules/auth-service"
CONCILIATION_SERVICE_DIR="modules/conciliation-service"

AUTH_JAR="$AUTH_SERVICE_DIR/target/auth-service-0.0.1-SNAPSHOT.jar"
CONCILIATION_JAR="$CONCILIATION_SERVICE_DIR/target/conciliation-service-0.0.1-SNAPSHOT.jar"

AUTH_IMAGE="auth-service-app:latest"
CONCILIATION_IMAGE="conciliation-service-app:latest"

AUTH_INFRA="infrastructure/auth/auth-app.yaml"
CONCILIATION_INFRA="infrastructure/conciliation/conciliation-app.yaml"

AUTH_DB_INFRA="infrastructure/postgres/postgres-pod.yaml"
CONCILIATION_DB_INFRA="infrastructure/postgres/conciliation-postgres-pod.yaml"

AUTH_POSTGRES_CONTAINER="auth-pod-auth-postgres"
CONCILIATION_POSTGRES_CONTAINER="conciliation-postgres-pod-conciliation-postgres"

# Paso 1: Compilar todo el proyecto
echo -e "${CYAN}📦 Compilando todo el proyecto Maven...${NC}"
./mvnw clean package -DskipTests > /dev/null

# Validar JARs
[[ -f "$AUTH_JAR" ]] || { echo -e "${RED}❌ JAR de auth-service no encontrado${NC}"; exit 1; }
[[ -f "$CONCILIATION_JAR" ]] || { echo -e "${RED}❌ JAR de conciliation-service no encontrado${NC}"; exit 1; }

echo -e "${GREEN}✅ JARs generados correctamente.${NC}"

# Paso 2: Construcción de imágenes
echo -e "${CYAN}🔨 Construyendo imagen auth-service...${NC}"
podman build -t "$AUTH_IMAGE" -f "$AUTH_SERVICE_DIR/Containerfile" "$AUTH_SERVICE_DIR" > /dev/null
echo -e "${GREEN}✅ Imagen auth-service lista.${NC}"

echo -e "${CYAN}🔨 Construyendo imagen conciliation-service...${NC}"
podman build -t "$CONCILIATION_IMAGE" -f "$CONCILIATION_SERVICE_DIR/Containerfile" "$CONCILIATION_SERVICE_DIR" > /dev/null
echo -e "${GREEN}✅ Imagen conciliation-service lista.${NC}"

# Paso 3: Iniciar PostgreSQL para auth
echo -e "${CYAN}🐘 Iniciando PostgreSQL para auth-service...${NC}"
podman play kube "$AUTH_DB_INFRA" > /dev/null

# Paso 4: Esperar PostgreSQL auth
echo -e "${YELLOW}⌛ Esperando PostgreSQL authdb...${NC}"
until podman exec "$AUTH_POSTGRES_CONTAINER" psql -U admin -d authdb -c '\q' > /dev/null 2>&1; do
  echo -e "${YELLOW}⏳ Esperando authdb...${NC}"
  sleep 2
done
echo -e "${GREEN}✅ PostgreSQL authdb listo.${NC}"

# Paso 5: Iniciar auth-service
echo -e "${CYAN}🚀 Iniciando auth-service...${NC}"
podman play kube "$AUTH_INFRA" > /dev/null
echo -e "${GREEN}✅ auth-service iniciado.${NC}"

# Paso 6: Iniciar PostgreSQL para conciliation
echo -e "${CYAN}🐘 Iniciando PostgreSQL para conciliation-service...${NC}"
podman play kube "$CONCILIATION_DB_INFRA" > /dev/null

# Paso 7: Esperar PostgreSQL conciliation
echo -e "${YELLOW}⌛ Esperando PostgreSQL conciliationdb...${NC}"
until podman exec "$CONCILIATION_POSTGRES_CONTAINER" psql -U admin -d conciliationdb -c '\q' > /dev/null 2>&1; do
  echo -e "${YELLOW}⏳ Esperando conciliationdb...${NC}"
  sleep 2
done
echo -e "${GREEN}✅ PostgreSQL conciliationdb listo.${NC}"

# Paso 8: Iniciar conciliation-service
echo -e "${CYAN}🚀 Iniciando conciliation-service...${NC}"
podman play kube "$CONCILIATION_INFRA" > /dev/null
echo -e "${GREEN}✅ conciliation-service iniciado.${NC}"

# Final
echo -e "${GREEN}🎉 Todos los servicios están levantados correctamente.${NC}"
