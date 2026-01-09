# 🚀 API Changelog - Módulo Ordem de Serviço (OS)

Este documento detalha as novas funcionalidades, endpoints e estruturas de dados implementadas para o módulo de **Ordem de Serviço** e **Relatórios PDF**.

---

## 1. Fluxo da Ordem de Serviço
O ciclo de vida da OS é controlado pelo campo `status`.

**Status Disponíveis (Enum):**
- `ABERTA`: Estado inicial (Rascunho).
- `EM_EXECUCAO`: Serviço iniciado.
- `FINALIZADA`: Serviço concluído e pago.

> **⚠️ Importante:** Ao mudar o status para `FINALIZADA`, o sistema **automaticamente** gera um registro de Faturamento (Receita) com o valor total da OS.

---

## 2. Endpoints e Payloads

### 🛠️ Clientes
Gestão de clientes para vincular à OS.

#### `POST /api/v1/clientes`
Cria um novo cliente.
**Payload:**
```json
{
  "razaoSocial": "Empresa X LTDA",
  "nomeFantasia": "X Tech",
  "cnpj": "00.000.000/0001-00",
  "endereco": "Rua A, 123",
  "contato": "(11) 99999-9999",
  "email": "contato@xtech.com"
}
```

#### `GET /api/v1/clientes`
Lista todos os clientes.

---

### 🛠️ Catálogo de Peças/Serviços
Itens padronizados que podem ser adicionados aos veículos.

#### `POST /api/v1/tipos-peca`
Adiciona um item ao catálogo.
**Payload:**
```json
{
  "nome": "Troca de Óleo",
  "valorPadrao": 150.00
}
```

#### `GET /api/v1/tipos-peca`
Lista o catálogo.

---

### 🛠️ Ordem de Serviço (Core)

#### `POST /api/v1/ordens-servico`
Cria uma nova OS (Status inicial: `ABERTA`).
**Payload:**
```json
{
  "clienteId": 1,
  "data": "2024-02-28"
}
```
**Response:** Retorna o objeto `OrdemServicoResponse` completo.

#### `POST /api/v1/ordens-servico/veiculos`
Adiciona um veículo a uma OS existente.
**Payload:**
```json
{
  "ordemServicoId": 1,
  "placa": "ABC-1234",
  "modelo": "Fiat Uno",
  "cor": "Branco"
}
```

#### `POST /api/v1/ordens-servico/pecas`
Adiciona uma peça/serviço a um veículo.
*Nota: `valorCobrado` é opcional. Se nulo, usa o valor padrão do catálogo.*
**Payload:**
```json
{
  "veiculoId": 10,
  "tipoPecaId": 5,
  "valorCobrado": 145.00 
}
```

#### `PATCH /api/v1/ordens-servico/{id}/status`
Atualiza o status da OS. Use isto para finalizar o serviço.
**Payload:**
```json
{
  "status": "FINALIZADA"
}
```

#### `GET /api/v1/ordens-servico/{id}`
Retorna os detalhes completos da OS (hierarquia: OS -> Veículos -> Peças).
**Exemplo de Response:**
```json
{
  "id": 1,
  "data": "2024-02-28",
  "status": "ABERTA",
  "valorTotal": 145.00,
  "cliente": { ... },
  "veiculos": [
    {
      "id": 10,
      "placa": "ABC-1234",
      "valorTotal": 145.00,
      "pecas": [
        {
          "id": 50,
          "nomePeca": "Troca de Óleo",
          "valorCobrado": 145.00
        }
      ]
    }
  ]
}
```

---

## 3. Relatórios PDF
Exportação do relatório financeiro mensal.

#### `GET /api/v1/relatorios/{ano}/{mes}/pdf`
- **Response Type:** `application/pdf`
- Gera download direto do arquivo.

---

## 💡 Dicas para o Frontend
1.  **Totais**: Os campos `valorTotal` na OS e nos Veículos são calculados automaticamente pelo backend. O front deve apenas exibi-los.
2.  **Preço Sugerido**: Ao selecionar uma peça do catálogo, o front pode pré-preencher o campo de valor com o `valorPadrao`, mas permitir edição (enviando `valorCobrado`).
3.  **Status**: Exiba o status atual da OS via Badge/Label. Destaque a transição para `FINALIZADA` pois ela gera impacto financeiro.
