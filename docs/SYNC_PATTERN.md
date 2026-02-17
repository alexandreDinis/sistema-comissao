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

## Checagem Leve (3G Friendly)

### Endpoint `/api/v1/sync/status`
Retorna os timestamps máximos de alteração por recurso, permitindo que o mobile saiba se há atualizações sem baixar dados.

**Request:** `GET /api/v1/sync/status` (Header: Authorization Bearer ...)

**Response:**
```json
{
  "serverTime": "2026-02-08T14:00:00Z",
  "clientesUpdatedAtMax": "2026-02-08T13:40:00Z",
  "osUpdatedAtMax": "2026-02-08T12:10:00Z"
}
```

### Fluxo Mobile
1.  **Check:** App chama `/sync/status`.
2.  **Compare:** Se `clientesUpdatedAtMax > last_synced_at_client`, exibe badge "Atualizações disponíveis".
3.  **Action:** Usuário clica em sincronizar -> App faz o sync incremental normal.
