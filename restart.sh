#!/bin/bash

set -euo pipefail

# Colores para mensajes
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m' # Sin color

# Verificación de existencia de scripts necesarios
if [[ ! -x "./stop.sh" ]]; then
  echo -e "${RED}❌ Error: No se encontró el script stop.sh o no tiene permisos de ejecución.${NC}"
  exit 1
fi

if [[ ! -x "./start.sh" ]]; then
  echo -e "${RED}❌ Error: No se encontró el script start.sh o no tiene permisos de ejecución.${NC}"
  exit 1
fi

echo -e "${CYAN}🔁 Reiniciando entorno de desarrollo...${NC}"

./stop.sh
echo -e "${YELLOW}⌛ Esperando limpieza antes de reiniciar...${NC}"
sleep 5
./start.sh

echo -e "${GREEN}✅ Entorno reiniciado exitosamente.${NC}"
