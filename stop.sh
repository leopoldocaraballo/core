#!/bin/bash

set -euo pipefail

# Colores para mensajes
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m' # Sin color

# Verificación de dependencias
if ! command -v podman &>/dev/null; then
  echo -e "${RED}❌ Error: 'podman' no está instalado o no está en el PATH.${NC}"
  exit 1
fi

echo -e "${CYAN}🛑 Deteniendo y eliminando contenedores/pods...${NC}"

# Función para eliminar pods
delete_pod() {
  local POD_NAME="$1"
  if podman pod exists "$POD_NAME"; then
    podman pod rm -f "$POD_NAME" > /dev/null
    echo -e "${GREEN}✅ Pod '${POD_NAME}' eliminado.${NC}"
  else
    echo -e "${YELLOW}⚠️  Pod '${POD_NAME}' no encontrado o ya eliminado.${NC}"
  fi
}

# Función para eliminar imágenes
delete_image() {
  local IMAGE_NAME="$1"
  if podman images | grep -q "$IMAGE_NAME"; then
    podman rmi -f "$IMAGE_NAME" > /dev/null
    echo -e "${GREEN}🗑 Imagen '${IMAGE_NAME}' eliminada.${NC}"
  else
    echo -e "${YELLOW}⚠️  Imagen '${IMAGE_NAME}' no encontrada.${NC}"
  fi
}

# Función para eliminar contenedores huérfanos (infraestructura temporal)
delete_orphaned_containers() {
  local CONTAINERS
  CONTAINERS=$(podman ps -a --format '{{.Names}}' | grep 'infra$' || true)
  for container in $CONTAINERS; do
    podman rm -f "$container" > /dev/null
    echo -e "${GREEN}🧽 Contenedor huérfano '${container}' eliminado.${NC}"
  done
}

# Lista de pods a eliminar
PODS=(
  "auth-app-pod"
  "auth-pod"
  "conciliation-app-pod"
  "conciliation-postgres-pod"
)



# Lista de imágenes a eliminar
IMAGES=(
  "auth-service-app:latest"
  "conciliation-service-app:latest"
)

for POD in "${PODS[@]}"; do
  delete_pod "$POD"
done

delete_orphaned_containers

for IMAGE in "${IMAGES[@]}"; do
  delete_image "$IMAGE"
done

echo -e "${CYAN}🧹 Limpieza finalizada.${NC}"
