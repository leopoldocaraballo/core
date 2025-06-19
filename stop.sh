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

# Lista de pods a eliminar (orden importa si hay dependencias de red/volúmenes)
PODS=(
  "auth-app-pod"
  "auth-pod"
)

for POD in "${PODS[@]}"; do
  delete_pod "$POD"
done

echo -e "${CYAN}🧹 Limpieza completada.${NC}"
