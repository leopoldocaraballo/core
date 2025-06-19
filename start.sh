#!/bin/bash

set -euo pipefail

# Colores para mensajes
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m' # Sin color

# Paths
AUTH_SERVICE_DIR="modules/auth-service"
JAR_FILE="$AUTH_SERVICE_DIR/target/auth-service-0.0.1-SNAPSHOT.jar"
POD_NAME="auth-app-pod"
POSTGRES_CONTAINER_NAME="auth-pod-auth-postgres"
APP_CONTAINER_NAME="auth-app-pod-auth-service"

# Paso 1: Compilar
echo -e "${CYAN}📦 Compilando todo el proyecto Maven...${NC}"
./mvnw clean package -DskipTests > /dev/null

if [[ ! -f "$JAR_FILE" ]]; then
  echo -e "${RED}❌ Error: No se generó el archivo JAR ejecutable en $JAR_FILE${NC}"
  exit 1
fi
echo -e "${GREEN}✅ JAR generado correctamente: $JAR_FILE${NC}"

# Paso 2: Crear pod con puertos expuestos
echo -e "${CYAN}🚀 Creando pod con puertos 8080 y 5432...${NC}"
podman pod exists "$POD_NAME" || podman pod create --name "$POD_NAME" -p 8080:8080 -p 5432:5432 > /dev/null

# Paso 3: Iniciar base de datos
echo -e "${CYAN}🚀 Iniciando base de datos...${NC}"
podman run -d \
  --rm=false \
  --pod "$POD_NAME" \
  --name "$POSTGRES_CONTAINER_NAME" \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=Vengal#2025 \
  -e POSTGRES_DB=authdb \
  docker.io/library/postgres:15 > /dev/null

# Paso 4: Esperar a que esté lista
echo -e "${YELLOW}⌛ Esperando a que PostgreSQL esté listo...${NC}"
until podman exec "$POSTGRES_CONTAINER_NAME" pg_isready -U admin > /dev/null 2>&1; do
  echo -e "${YELLOW}⏳ Esperando PostgreSQL...${NC}"
  sleep 2
done
echo -e "${GREEN}✅ PostgreSQL está listo.${NC}"

# Paso 5: Construir imagen de auth-service
echo -e "${CYAN}🔨 Construyendo imagen auth-service-app desde Containerfile...${NC}"
podman build -t auth-service-app:latest -f "$AUTH_SERVICE_DIR/Containerfile" "$AUTH_SERVICE_DIR" > /dev/null

# Paso 6: Iniciar Auth Service
echo -e "${CYAN}🚀 Iniciando Auth Service...${NC}"
podman run -d \
  --rm=false \
  --pod "$POD_NAME" \
  --name "$APP_CONTAINER_NAME" \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://$POSTGRES_CONTAINER_NAME:5432/authdb \
  -e SPRING_DATASOURCE_USERNAME=admin \
  -e SPRING_DATASOURCE_PASSWORD=Vengal#2025 \
  auth-service-app:latest > /dev/null

echo -e "${GREEN}✅ Todos los servicios están levantados correctamente.${NC}"
