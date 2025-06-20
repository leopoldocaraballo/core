#!/bin/bash
set -euo pipefail

# 🎨 Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${CYAN}🛑 Deteniendo y limpiando entorno...${NC}"

# ==============================
# 🔧 Funciones
# ==============================

require_dependency() {
  if ! command -v "$1" &>/dev/null; then
    echo -e "${RED}❌ Error: '$1' no está instalado o en el PATH.${NC}"
    exit 1
  fi
}

delete_pod() {
  local pod="$1"
  if podman pod exists "$pod"; then
    podman pod rm -f "$pod" > /dev/null
    echo -e "${GREEN}✅ Pod '${pod}' eliminado.${NC}"
  else
    echo -e "${YELLOW}⚠️  Pod '${pod}' no existe.${NC}"
  fi
}

delete_image_if_unused() {
  local image="$1"
  if podman images --format "{{.Repository}}:{{.Tag}}" | grep -qx "$image"; then
    if podman ps -a --format "{{.Image}}" | grep -q "$image"; then
      echo -e "${YELLOW}⚠️  Imagen '${image}' está en uso. No se elimina.${NC}"
    else
      podman rmi -f "$image" > /dev/null
      echo -e "${GREEN}🗑 Imagen '${image}' eliminada.${NC}"
    fi
  else
    echo -e "${YELLOW}⚠️  Imagen '${image}' no encontrada.${NC}"
  fi
}

delete_infra_containers() {
  podman ps -a --format '{{.Names}}' | grep 'infra$' || return
  for orphan in $(podman ps -a --format '{{.Names}}' | grep 'infra$'); do
    podman rm -f "$orphan" > /dev/null
    echo -e "${GREEN}🧽 Contenedor huérfano '${orphan}' eliminado.${NC}"
  done
}

# ==============================
# 🔍 Verificación de dependencias
# ==============================
require_dependency podman

# ==============================
# 📦 Listas
# ==============================
PODS=(
  "auth-app-pod"
  "auth-pod"
  "conciliation-app-pod"
  "conciliation-postgres-pod"
)

IMAGES=(
  "auth-service-app:latest"
  "conciliation-service-app:latest"
)

# ==============================
# 🧹 Limpieza
# ==============================

for pod in "${PODS[@]}"; do
  delete_pod "$pod"
done

delete_infra_containers

for image in "${IMAGES[@]}"; do
  delete_image_if_unused "$image"
done

echo -e "${GREEN}✅ Limpieza completa.${NC}"
