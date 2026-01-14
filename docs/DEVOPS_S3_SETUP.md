# Guia DevOps - Configuração S3 Storage

## Objetivo

Configurar armazenamento de arquivos (logos de empresas) em serviço compatível com S3.

---

## Variáveis de Ambiente Necessárias

| Variável | Descrição | Exemplo |
|----------|-----------|---------|
| `AWS_S3_ACCESS_KEY` | Chave de acesso | `AKIAIOSFODNN7EXAMPLE` |
| `AWS_S3_SECRET_KEY` | Chave secreta | `wJalrXUtnFEMI/K7MDENG...` |
| `AWS_S3_BUCKET` | Nome do bucket | `sistema-comissao-logos` |
| `AWS_S3_REGION` | Região (opcional) | `us-east-1` |
| `AWS_S3_ENDPOINT` | Endpoint customizado | Para R2/MinIO |
| `AWS_S3_PUBLIC_URL` | URL pública CDN | Para acesso direto |

---

## Opções de Provedor (Menor Custo)

### 🥇 Cloudflare R2 (RECOMENDADO - Custo Zero)

**Free tier**: 10GB armazenamento + 10M requests/mês

```bash
# Variáveis para R2
AWS_S3_ACCESS_KEY=<R2_ACCESS_KEY_ID>
AWS_S3_SECRET_KEY=<R2_SECRET_ACCESS_KEY>
AWS_S3_BUCKET=sistema-comissao-logos
AWS_S3_REGION=auto
AWS_S3_ENDPOINT=https://<ACCOUNT_ID>.r2.cloudflarestorage.com
AWS_S3_PUBLIC_URL=https://pub-<xxx>.r2.dev
```

**Passos**:
1. Acesse [Cloudflare Dashboard](https://dash.cloudflare.com) → R2 → Create Bucket
2. Nome: `sistema-comissao-logos`
3. Vá em "Manage R2 API Tokens" → Create API Token
4. Permissão: "Object Read & Write"
5. (Opcional) Habilitar "Public Access" para URLs públicas

---

### 🥈 Backblaze B2 (Também Custo Zero)

**Free tier**: 10GB armazenamento + 1GB download/dia

```bash
AWS_S3_ACCESS_KEY=<B2_KEY_ID>
AWS_S3_SECRET_KEY=<B2_APPLICATION_KEY>
AWS_S3_BUCKET=sistema-comissao-logos
AWS_S3_REGION=us-west-004
AWS_S3_ENDPOINT=https://s3.us-west-004.backblazeb2.com
AWS_S3_PUBLIC_URL=https://f004.backblazeb2.com/file/sistema-comissao-logos
```

---

### 🥉 AWS S3 (Pago, mas baixo custo)

**Estimativa**: ~$0.02/GB/mês

```bash
AWS_S3_ACCESS_KEY=<AWS_ACCESS_KEY>
AWS_S3_SECRET_KEY=<AWS_SECRET_KEY>
AWS_S3_BUCKET=sistema-comissao-logos
AWS_S3_REGION=sa-east-1
# AWS_S3_ENDPOINT não necessário
# AWS_S3_PUBLIC_URL não necessário se usar proxy via backend
```

---

## Configuração do Bucket

### Política CORS (Obrigatória)

```json
{
  "CORSRules": [
    {
      "AllowedOrigins": ["https://sistema-comi-front.vercel.app"],
      "AllowedMethods": ["GET", "PUT", "POST"],
      "AllowedHeaders": ["*"],
      "MaxAgeSeconds": 3600
    }
  ]
}
```

### Estrutura de Pastas

```
bucket/
└── logos/
    ├── empresa-1-1705263600000.png
    ├── empresa-2-1705263700000.jpg
    └── ...
```

---

## Verificação

```bash
# Teste de conectividade (usando AWS CLI com endpoint customizado)
aws s3 ls s3://sistema-comissao-logos \
  --endpoint-url=$AWS_S3_ENDPOINT

# Ou via curl após deploy
curl -X POST \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@test-logo.png" \
  https://api.sistema-comissao.com/api/v1/empresa/1/logo
```

---

## Railway/Render

Adicione as variáveis acima no painel de variáveis de ambiente do serviço.

**Railway**:
```
Settings → Variables → Add Variable
```

**Render**:
```
Environment → Environment Variables → Add
```

---

## Comparativo de Custos

| Provedor | Free Tier | Custo Após |
|----------|-----------|------------|
| Cloudflare R2 | 10GB + 10M req | $0.015/GB |
| Backblaze B2 | 10GB | $0.005/GB |
| AWS S3 | Nenhum | $0.023/GB |
| MinIO (Self-hosted) | ∞ | Custo servidor |

> **Recomendação**: Para startups e MVPs, **Cloudflare R2** oferece o melhor custo-benefício com zero taxas de egress.
