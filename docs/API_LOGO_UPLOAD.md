# API Changelog - Logo Upload Feature

**Data**: 2026-01-14  
**Versão**: 1.1.0

---

## Novos Endpoints

### Upload de Logo da Empresa

```http
POST /api/v1/empresa/{id}/logo
```

**Autenticação**: Bearer Token (JWT)  
**Permissão**: `ADMIN_EMPRESA` (somente pode atualizar sua própria empresa)

**Request**:
- Content-Type: `multipart/form-data`
- Campo: `file` (obrigatório)

**Validações**:
| Regra | Descrição |
|-------|-----------|
| Tipos | PNG, JPEG, WebP |
| Tamanho | Máximo 2MB |

**Response (200)**:
```json
{
  "message": "Logo atualizado com sucesso",
  "logoUrl": "https://storage.example.com/logos/empresa-1-1705263600000.png"
}
```

**Erros**:
| Código | Descrição |
|--------|-----------|
| 400 | Arquivo inválido (tipo ou tamanho) |
| 403 | Tentando atualizar logo de outra empresa |
| 500 | Erro interno no upload |

---

### Visualizar Logo da Empresa

```http
GET /api/v1/empresa/{id}/logo
```

**Autenticação**: Não requerida (público)

**Response**: Binário da imagem com `Content-Type` apropriado

**Erros**:
| Código | Descrição |
|--------|-----------|
| 404 | Empresa ou logo não encontrado |

---

## Alterações em Endpoints Existentes

### GET /api/v1/empresa/config

**Novo campo na resposta**:
```json
{
  "id": 1,
  "nome": "Empresa Exemplo",
  "modoComissao": "INDIVIDUAL",
  "logoUrl": "https://storage.example.com/logos/empresa-1.png"
}
```

> **Nota**: `logoUrl` será `null` se a empresa não tiver logo.

---

## Exemplo de Integração (TypeScript)

```typescript
// Upload de logo
async function uploadLogo(empresaId: number, file: File): Promise<void> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`/api/v1/empresa/${empresaId}/logo`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error);
  }
}

// Exibir logo
function getLogoUrl(empresaId: number): string {
  return `/api/v1/empresa/${empresaId}/logo`;
}
```

---

## Tipos TypeScript

```typescript
interface EmpresaConfig {
  id: number;
  nome: string;
  modoComissao: 'INDIVIDUAL' | 'COLETIVA';
  logoUrl: string | null;  // NOVO CAMPO
}

interface UploadLogoResponse {
  message: string;
  logoUrl: string;
}
```

---

## PDF de Ordem de Serviço

### Download PDF da OS

```http
GET /api/v1/ordens-servico/{id}/pdf
```

**Autenticação**: Bearer Token (JWT)

**Response**: Arquivo PDF binário

**Headers da Response**:
- `Content-Type: application/pdf`
- `Content-Disposition: attachment; filename="OS-{id}.pdf"`

**Conteúdo do PDF**:
- Cabeçalho com logo e dados da empresa
- Dados do cliente
- Veículos com placa, modelo, cor
- Serviços/peças por veículo
- Totais com desconto (se aplicável)
- Status destacado com cores

**Erros**:
| Código | Descrição |
|--------|-----------|
| 404 | OS não encontrada |

---

### Exemplo de Integração (Download PDF)

```typescript
async function downloadOSPdf(osId: number): Promise<void> {
  const response = await fetch(`/api/v1/ordens-servico/${osId}/pdf`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });

  if (!response.ok) {
    throw new Error('Erro ao gerar PDF');
  }

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  
  const a = document.createElement('a');
  a.href = url;
  a.download = `OS-${osId}.pdf`;
  a.click();
  
  window.URL.revokeObjectURL(url);
}
```
