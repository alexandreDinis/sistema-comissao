# API de Cartões de Crédito - Documentação Frontend

## Base URL
```
/api/v1/cartoes
```

## Autenticação
Todos os endpoints requerem autenticação via JWT Bearer Token.

---

## Endpoints

### 1. Listar Cartões
```http
GET /api/v1/cartoes
```

**Autorização:** `ADMIN` ou `ADMIN_EMPRESA`

**Response 200:**
```json
[
  {
    "id": 1,
    "nome": "Cartão Corporativo",
    "diaVencimento": 10,
    "diaFechamento": 25,
    "limite": 5000.00,
    "ativo": true,
    "dataCriacao": "2026-01-24T10:00:00",
    "dataAtualizacao": "2026-01-24T10:00:00"
  }
]
```

---

### 2. Criar Cartão
```http
POST /api/v1/cartoes
```

**Autorização:** `ADMIN`

**Request Body:**
```json
{
  "nome": "Cartão Nubank",
  "diaVencimento": 15,
  "diaFechamento": 8,
  "limite": 10000.00
}
```

| Campo | Tipo | Obrigatório | Descrição |
|-------|------|-------------|-----------|
| nome | string | Sim | Nome do cartão |
| diaVencimento | integer | Sim | Dia do vencimento da fatura (1-31) |
| diaFechamento | integer | Não | Dia do fechamento (default: 25) |
| limite | decimal | Não | Limite do cartão (null = sem limite) |

**Response 200:** Cartão criado

---

### 3. Editar Cartão ✨ NOVO
```http
PUT /api/v1/cartoes/{id}
```

**Autorização:** `ADMIN`

**Request Body:**
```json
{
  "nome": "Cartão Nubank Atualizado",
  "diaVencimento": 20,
  "diaFechamento": 10,
  "limite": 15000.00
}
```

**Response 200:** Cartão atualizado

**Response 400:** 
```json
{
  "message": "Cartão não encontrado"
}
```

---

### 4. Consultar Limite Disponível ✨ NOVO
```http
GET /api/v1/cartoes/{id}/limite-disponivel
```

**Autorização:** `ADMIN` ou `ADMIN_EMPRESA`

**Response 200:**
```json
{
  "limiteTotal": 10000.00,
  "limiteDisponivel": 7500.00,
  "limiteUtilizado": 2500.00
}
```

> **Nota:** Se o cartão não tiver limite definido, todos os valores serão `null`.

---

### 5. Desativar Cartão
```http
DELETE /api/v1/cartoes/{id}
```

**Autorização:** `ADMIN`

**Response 204:** Cartão desativado (soft delete)

---

## Regras de Negócio

### Limite de Cartão
- Se `limite` for definido, o sistema valida antes de criar despesas
- Ao tentar criar despesa que exceda o limite:
  ```json
  {
    "message": "Limite insuficiente. Disponivel: R$ 500.00, Necessario: R$ 1000.00"
  }
  ```
- Quando a fatura é **paga**, o limite é automaticamente liberado

### Fechamento de Fatura
- Despesas criadas **após** o dia de fechamento vão para a fatura do mês seguinte
- Exemplo: Cartão fecha dia 25 → Despesa dia 26/01 vai para fatura de 02/2026

---

## Exemplo de Serviço TypeScript

```typescript
// cartaoService.ts
import api from './api';

export interface CartaoCredito {
  id: number;
  nome: string;
  diaVencimento: number;
  diaFechamento: number;
  limite: number | null;
  ativo: boolean;
  dataCriacao: string;
  dataAtualizacao: string;
}

export interface CartaoRequest {
  nome: string;
  diaVencimento: number;
  diaFechamento?: number;
  limite?: number;
}

export interface LimiteDisponivelDTO {
  limiteTotal: number | null;
  limiteDisponivel: number | null;
  limiteUtilizado: number | null;
}

export const cartaoService = {
  listar: () => 
    api.get<CartaoCredito[]>('/api/v1/cartoes'),

  criar: (data: CartaoRequest) => 
    api.post<CartaoCredito>('/api/v1/cartoes', data),

  editar: (id: number, data: CartaoRequest) => 
    api.put<CartaoCredito>(`/api/v1/cartoes/${id}`, data),

  getLimiteDisponivel: (id: number) => 
    api.get<LimiteDisponivelDTO>(`/api/v1/cartoes/${id}/limite-disponivel`),

  desativar: (id: number) => 
    api.delete(`/api/v1/cartoes/${id}`),
};
```

---

## Componente de Exemplo (React)

```tsx
// CartaoForm.tsx
interface CartaoFormProps {
  cartao?: CartaoCredito;
  onSave: (data: CartaoRequest) => void;
  onCancel: () => void;
}

export function CartaoForm({ cartao, onSave, onCancel }: CartaoFormProps) {
  const [form, setForm] = useState<CartaoRequest>({
    nome: cartao?.nome || '',
    diaVencimento: cartao?.diaVencimento || 10,
    diaFechamento: cartao?.diaFechamento || 25,
    limite: cartao?.limite || undefined,
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    onSave(form);
  };

  return (
    <form onSubmit={handleSubmit}>
      <input 
        placeholder="Nome do cartão"
        value={form.nome}
        onChange={e => setForm({...form, nome: e.target.value})}
        required
      />
      <input 
        type="number"
        placeholder="Dia de vencimento"
        value={form.diaVencimento}
        onChange={e => setForm({...form, diaVencimento: +e.target.value})}
        min={1} max={31}
        required
      />
      <input 
        type="number"
        placeholder="Dia de fechamento"
        value={form.diaFechamento}
        onChange={e => setForm({...form, diaFechamento: +e.target.value})}
        min={1} max={31}
      />
      <input 
        type="number"
        placeholder="Limite (opcional)"
        value={form.limite || ''}
        onChange={e => setForm({...form, limite: e.target.value ? +e.target.value : undefined})}
        step="0.01"
      />
      <button type="submit">{cartao ? 'Salvar' : 'Criar'}</button>
      <button type="button" onClick={onCancel}>Cancelar</button>
    </form>
  );
}
```
