# Padrão de Incremental Sync

## Objetivo
Sincronizar mudanças (delta) entre servidor e mobile sem perder dados.

## Implementação

### Backend
1. **Controller**: Recebe `Instant since` (UTC com "Z")
2. **SyncUtils**: Converte para `LocalDateTime` local + skew -2s
3. **Repository**: Filtra `WHERE updated_at > :since AND empresa_id = :empresa`
4. **Entity**: Garante `@PreUpdate` atualiza `updated_at`

### Mobile
1. **Cursor**: Salva `last_synced_at = syncStart` (antes da API)
2. **Deduplicação**: Upsert por `server_id`
3. **Processamento**: Em chunks pra não travar SQLite

## Entidades com Incremental
- ✅ Cliente (`/clientes?since=...`)
- ✅ OrdemServico (`/ordens-servico?since=...`)

## Próximas Entidades (quando necessário)
- Faturamento
- ContaReceber
- Despesas
