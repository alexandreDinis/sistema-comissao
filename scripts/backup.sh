#!/bin/bash
# =====================================================
# Script de Backup Automático - PostgreSQL
# =====================================================
# Uso: ./backup.sh
# Cron: 0 3 * * * /opt/sistema-comissao/backup.sh >> /var/log/backup.log 2>&1
# =====================================================

set -e

# Configurações
BACKUP_DIR="/opt/sistema-comissao/backups"
CONTAINER_NAME="sistema-db"
DB_NAME="${DB_NAME:-comissaodb}"
DB_USER="${DB_USER:-comissao}"
RETENTION_DAYS=7
DATE=$(date +%Y-%m-%d_%H-%M-%S)
BACKUP_FILE="${BACKUP_DIR}/backup_${DB_NAME}_${DATE}.sql.gz"

# Criar diretório se não existir
mkdir -p "$BACKUP_DIR"

echo "[$(date)] Iniciando backup do banco ${DB_NAME}..."

# Executar pg_dump dentro do container e comprimir
docker exec -t "$CONTAINER_NAME" pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$BACKUP_FILE"

# Verificar se backup foi criado
if [ -f "$BACKUP_FILE" ]; then
    SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    echo "[$(date)] Backup criado com sucesso: $BACKUP_FILE ($SIZE)"
else
    echo "[$(date)] ERRO: Falha ao criar backup!"
    exit 1
fi

# Remover backups antigos
echo "[$(date)] Removendo backups com mais de ${RETENTION_DAYS} dias..."
find "$BACKUP_DIR" -name "backup_*.sql.gz" -mtime +$RETENTION_DAYS -delete

# Listar backups restantes
echo "[$(date)] Backups disponíveis:"
ls -lh "$BACKUP_DIR"/backup_*.sql.gz 2>/dev/null || echo "Nenhum backup encontrado"

echo "[$(date)] Backup concluído!"
