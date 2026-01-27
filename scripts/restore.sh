#!/bin/bash
# =====================================================
# Script de Restauração de Backup - PostgreSQL
# =====================================================
# Uso: ./restore.sh backup_comissaodb_2026-01-27_03-00-00.sql.gz
# =====================================================

set -e

BACKUP_DIR="/opt/sistema-comissao/backups"
CONTAINER_NAME="sistema-db"
DB_NAME="${DB_NAME:-comissaodb}"
DB_USER="${DB_USER:-comissao}"

if [ -z "$1" ]; then
    echo "Uso: $0 <arquivo_backup.sql.gz>"
    echo ""
    echo "Backups disponíveis:"
    ls -lh "$BACKUP_DIR"/backup_*.sql.gz 2>/dev/null || echo "Nenhum backup encontrado"
    exit 1
fi

BACKUP_FILE="$1"

# Verificar se é caminho absoluto ou relativo
if [[ ! "$BACKUP_FILE" = /* ]]; then
    BACKUP_FILE="${BACKUP_DIR}/${BACKUP_FILE}"
fi

if [ ! -f "$BACKUP_FILE" ]; then
    echo "ERRO: Arquivo não encontrado: $BACKUP_FILE"
    exit 1
fi

echo "⚠️  ATENÇÃO: Isso irá SUBSTITUIR todos os dados do banco ${DB_NAME}!"
read -p "Tem certeza? (digite 'sim' para continuar): " CONFIRM

if [ "$CONFIRM" != "sim" ]; then
    echo "Operação cancelada."
    exit 0
fi

echo "[$(date)] Iniciando restauração de: $BACKUP_FILE"

# Descomprimir e restaurar
gunzip -c "$BACKUP_FILE" | docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME"

echo "[$(date)] Restauração concluída com sucesso!"
