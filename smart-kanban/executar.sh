#!/usr/bin/env bash
# Carrega as variáveis do arquivo .env (se existir) e inicia o sistema.
cd "$(dirname "$0")"
if [ -f .env ]; then set -a; . ./.env; set +a; else echo "Aviso: .env não encontrado. A IA ficará desativada."; fi
mvn spring-boot:run
