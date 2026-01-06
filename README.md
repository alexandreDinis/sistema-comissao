# Sistema de Controle de Comissão

Sistema para gerenciamento de comissões de vendas baseado em faixas de faturamento mensal.

## 🚀 Tecnologias

- Java 17
- Spring Boot 3.2.1
- Spring Data JPA
- PostgreSQL / H2
- Flyway
- Maven
- Docker & Docker Compose
- Swagger/OpenAPI

## 📋 Pré-requisitos

- JDK 17 ou superior
- Maven 3.6+
- Docker e Docker Compose (para executar o PostgreSQL)

## 🔧 Instalação

### 1. Clone o repositório

```bash
git clone https://github.com/sua-empresa/sistema-comissao.git
cd sistema-comissao
```

### 2. Inicie o banco de dados (Produção)

```bash
docker-compose up -d
```

### 3. Execute a aplicação

**Modo Desenvolvimento (H2):**
```bash
mvn spring-boot:run
```

**Modo Produção (PostgreSQL):**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### 4. Acesse a aplicação

- API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/api/swagger-ui.html
- H2 Console (dev): http://localhost:8080/api/h2-console

## 📚 Endpoints da API

### Faturamento

- `POST /api/v1/faturamento` - Registrar novo faturamento
- `GET /api/v1/faturamento` - Listar todos os faturamentos

### Adiantamento

- `POST /api/v1/adiantamento` - Registrar novo adiantamento
- `GET /api/v1/adiantamento` - Listar todos os adiantamentos

### Comissão

- `GET /api/v1/comissao/{ano}/{mes}` - Calcular/obter comissão mensal

## 🧪 Testes

```bash
# Executar todos os testes
mvn test

# Executar testes com cobertura
mvn clean test jacoco:report
```

## 📦 Build

```bash
# Gerar o JAR
mvn clean package

# Executar o JAR
java -jar target/sistema-comissao-1.0.0-SNAPSHOT.jar
```

## 🤝 Contribuindo

1. Faça um Fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/NovaFuncionalidade`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova funcionalidade'`)
4. Push para a branch (`git push origin feature/NovaFuncionalidade`)
5. Abra um Pull Request

## 📄 Licença

Este projeto está sob a licença MIT.
