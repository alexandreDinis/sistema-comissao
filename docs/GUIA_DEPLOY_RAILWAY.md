# Guia de Deploy - Railway

Este documento contém as configurações recomendadas para deploy do sistema no Railway.

## Requisitos do Sistema

- **Java**: 21
- **Spring Boot**: 3.2.1
- **Banco de Dados**: PostgreSQL (provisionado pelo Railway)
- **Storage**: S3 compatível (Cloudflare R2, MinIO, etc.)

---

## Configuração da Instância

### Recursos Recomendados

| Configuração | Valor | Justificativa |
|--------------|-------|---------------|
| **Memory Limit** | 1 GB | Sweet spot para 2-5 usuários |
| **vCPU** | Shared (0.5-1) | Suficiente para carga leve |
| **Replicas** | 1 | Não necessário escalar inicialmente |

**Custo estimado**: $5-10/mês (baseado em uso)

---

## Variáveis de Ambiente

### JVM - Configuração de Memória

```bash
JAVA_TOOL_OPTIONS=-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication
```

**Explicação dos parâmetros:**

| Parâmetro | Descrição |
|-----------|-----------|
| `-Xms256m` | Heap inicial de 256MB (evita resizes no startup) |
| `-Xmx512m` | Heap máximo de 512MB |
| `-XX:+UseG1GC` | Garbage Collector otimizado para heaps <4GB |
| `-XX:MaxGCPauseMillis=100` | Limita pausas do GC para 100ms |
| `-XX:+UseStringDeduplication` | Reduz uso de memória deduplicando Strings |

### Startup Mais Rápido (Opcional)

Se o health check estiver falhando por timeout no cold start:

```bash
JAVA_TOOL_OPTIONS=-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication -XX:TieredStopAtLevel=1
```

> O `-XX:TieredStopAtLevel=1` reduz otimizações JIT, acelerando o startup.

---

### Banco de Dados (PostgreSQL)

O Railway provisiona automaticamente. Configure:

```bash
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=${DATABASE_URL}
# ou use as variáveis separadas:
SPRING_DATASOURCE_URL=jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}
SPRING_DATASOURCE_USERNAME=${PGUSER}
SPRING_DATASOURCE_PASSWORD=${PGPASSWORD}
```

---

### Storage S3 (Cloudflare R2 / MinIO)

```bash
S3_ENDPOINT=https://seu-account-id.r2.cloudflarestorage.com
S3_ACCESS_KEY=sua-access-key
S3_SECRET_KEY=sua-secret-key
S3_BUCKET=nome-do-bucket
S3_REGION=auto
```

> Ver [DEVOPS_S3_SETUP.md](./DEVOPS_S3_SETUP.md) para mais detalhes.

---

### JWT e Segurança

```bash
JWT_SECRET=sua-chave-secreta-muito-longa-e-segura-aqui
JWT_EXPIRATION=86400000
```

---

## Health Check

Configure o health check no Railway:

| Configuração | Valor |
|--------------|-------|
| **Path** | `/actuator/health` |
| **Timeout** | 120s (cold starts podem demorar) |
| **Interval** | 30s |

---

## Troubleshooting

### OutOfMemoryError durante geração de PDF

Aumente o heap máximo:

```bash
JAVA_TOOL_OPTIONS=-Xms256m -Xmx768m -XX:+UseG1GC
```

Também aumente o Memory Limit para 1.5GB no Railway.

### Startup lento / Health check falhando

1. Adicione `-XX:TieredStopAtLevel=1` nas JAVA_TOOL_OPTIONS
2. Aumente o timeout do health check para 180s
3. Verifique a conexão com o banco de dados

### Conexão com banco recusada

Verifique se está usando a variável correta:
- Railway fornece `DATABASE_URL` no formato `postgresql://user:pass@host:port/db`
- Spring espera `jdbc:postgresql://host:port/db`

A configuração `application-prod.properties` deve converter automaticamente.

---

## Checklist de Deploy

- [ ] Variáveis de ambiente configuradas
- [ ] PostgreSQL provisionado
- [ ] S3/R2 configurado (para logos)
- [ ] Memory limit em 1GB
- [ ] Health check configurado
- [ ] Domínio customizado (opcional)
