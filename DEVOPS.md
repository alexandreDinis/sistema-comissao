# Deploy no Railway - Passo a Passo Simples (Sem Docker)

Este guia descreve como realizar o deploy do Backend (Spring Boot) e Banco de Dados (PostgreSQL) usando o Railway.

## Visão Geral
*   **Backend**: Spring Boot (Java 17)
*   **Banco de Dados**: PostgreSQL (Gerenciado pelo Railway)
*   **Deploy**: Automático via GitHub

---

## 📌 Passo a Passo

### 1. Crie sua conta
1.  Acesse [https://railway.app](https://railway.app)
2.  Faça login com sua conta do **GitHub**.

### 2. Suba o Backend no GitHub
Certifique-se de que seu repositório contém os seguintes arquivos na raiz ou diretório indicado:
*   `/src`
*   `pom.xml`
*   `mvnw` (Maven Wrapper)

*⚠️ Não é necessário criar um Dockerfile.*

### 3. Criar Projeto no Railway
1.  No painel do Railway, clique em **New Project**.
2.  Selecione **Deploy from GitHub Repo**.
3.  Escolha o repositório do seu backend (`sistema-comissao`).

### 4. Adicionar PostgreSQL
1.  Dentro do projeto criado no Railway, clique em **+ New**.
2.  Selecione **Database** → **PostgreSQL**.
3.  O Railway criará e iniciará o banco de dados automaticamente.

### 5. Configurar Variáveis de Ambiente (CRÍTICO)
No serviço do seu Backend (não no PostgreSQL), vá até a aba **Variables** e adicione:

| Variável | Valor |
| :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `SPRING_DATASOURCE_URL` | `${{Postgres.DATABASE_URL}}` |
| `SPRING_DATASOURCE_USERNAME` | `${{Postgres.USER}}` |
| `SPRING_DATASOURCE_PASSWORD` | `${{Postgres.PASSWORD}}` |
| `JWT_SECRET` | *Uma chave muito longa e segura* |

*Nota: Ao usar `${{Postgres...}}`, o Railway preenche automaticamente com as credenciais do banco criado no passo 4.*

### 6. Finalizar
*   O Railway detectará automaticamente que é um projeto **Maven/Spring Boot**.
*   Ele rodará o comando de build (`mvn package`).
*   Iniciará o arquivo `.jar` gerado.

O deploy estará concluído quando você ver o log de "Application Started". O Railway fornecerá uma URL pública (ex: `https://sistema-comissao-production.up.railway.app`).
