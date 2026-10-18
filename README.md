# Selecta API — Processos Seletivos Mais Transparentes e Humanizados

API REST para um processo seletivo em que o candidato **acompanha cada etapa da candidatura**, recebe **feedbacks**, **agenda entrevistas** e conversa com a empresa em **tempo real**, em vez de ficar sem resposta depois de enviar o currículo.

O repositório contém o **backend** (`backend/`), construído com Spring Boot.

---

## Sumário

- [Funcionalidades](#funcionalidades)
- [Como funciona o processo seletivo](#como-funciona-o-processo-seletivo)
- [Tecnologias](#tecnologias)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Como executar](#como-executar)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Endpoints principais](#endpoints-principais)
- [Chat em tempo real (WebSocket)](#chat-em-tempo-real-websocket)
- [Testes](#testes)
- [Segurança](#segurança)

---

## Funcionalidades

**Candidatos**
- Cadastro e login (e-mail/senha com verificação por código, ou OAuth2 com **Google** e **LinkedIn**)
- Perfil completo: dados pessoais, experiências, projetos, avatar e currículo
- Busca de vagas com filtros (título, categoria, modalidade, faixa salarial, cidade, estado) e **busca por proximidade** (raio em km)
- Candidatura a vagas e acompanhamento da etapa e do status do processo
- Visualização de feedbacks e histórico da candidatura
- Confirmação ou recusa de agendamentos de entrevista
- Chat com a empresa dentro de cada candidatura
- Notificações

**Empresas**
- Cadastro de empresa e perfil com logo
- Criação e gerenciamento de vagas, com **etapas configuráveis** do processo
- Painel de candidaturas: avançar etapa, aprovar ou reprovar
- Envio de feedbacks aos candidatos
- Agendamento de entrevistas e testes
- Chat com candidatos

**Plataforma**
- E-mails transacionais assíncronos (confirmação de conta e atualizações da candidatura) via fila
- Notificações internas e mensagens em tempo real
- Documentação interativa com Swagger UI

---

## Como funciona o processo seletivo

Cada candidatura tem uma **etapa** (onde o candidato está) e um **status** (como ele está).

**Etapas** (só avançam para frente):

| Ordem | Etapa | Descrição |
|---|---|---|
| 0 | `INSCRICAO` | Inscrição realizada |
| 1 | `TRIAGEM` | Triagem de currículo |
| 2 | `ENTREVISTA_RH` | Entrevista com RH |
| 3 | `TESTE_TECNICO` | Teste técnico |
| 4 | `ENTREVISTA_TECNICA` | Entrevista técnica |
| 5 | `PROPOSTA` | Proposta |
| 6 | `CONTRATACAO` | Contratação |

Ao criar uma vaga, a empresa escolhe quais etapas intermediárias usar (sem `INSCRICAO` e `CONTRATACAO`, sem repetir e sem etapa nula). Se não informar nenhuma, vale o padrão: `TRIAGEM → ENTREVISTA_RH → TESTE_TECNICO → ENTREVISTA_TECNICA → PROPOSTA`.

**Status da candidatura:** `EM_ANDAMENTO`, `APROVADA`, `REPROVADA`, `CANCELADA` (pelo candidato).

**Status do agendamento:** `PENDENTE`, `CONFIRMADO`, `RECUSADO`, `CANCELADO`, `REALIZADO`, `NAO_COMPARECEU`.

**Modalidades de vaga:** `PRESENCIAL`, `REMOTO`, `HIBRIDO`.

**Perfis de usuário:** `CANDIDATE`, `COMPANY`, `ADMIN`.

---

## Tecnologias

| Área | Tecnologia |
|---|---|
| Linguagem | Java 25 |
| Framework | Spring Boot 4.1.0 (Web MVC, Data JPA, Validation) |
| Segurança | Spring Security, JWT assinado com RSA, OAuth2 Login (Google e LinkedIn) |
| Banco de dados | PostgreSQL 16 |
| Mensageria | RabbitMQ 4 (com filas *dead letter* e retry) |
| Tempo real | WebSocket / STOMP |
| E-mail | [Resend](https://resend.com) |
| Armazenamento de arquivos | Cloudflare R2 (SDK S3 da AWS) |
| Geocodificação | Nominatim (OpenStreetMap) |
| Documentação | springdoc-openapi (Swagger UI) |
| Build | Maven (wrapper incluso) |
| Testes | JUnit 5, Mockito, Spring Security Test |
| Containers | Docker e Docker Compose |

---

## Estrutura do projeto

```
backend/
├── Dockerfile
├── docker-compose.yml          # PostgreSQL + RabbitMQ para desenvolvimento
├── .env.example                # modelo das variáveis de ambiente
├── pom.xml
└── src/
    ├── main/java/com/unisal/selectaapi/
    │   ├── auth/               # login, cadastro de empresa, verificação de e-mail, OAuth2
    │   ├── user/               # usuários e perfis de acesso
    │   ├── candidate/          # perfil do candidato, experiências, projetos, avatar e currículo
    │   ├── company/            # perfil da empresa e logo
    │   ├── vagas/              # vagas, etapas, filtros e busca por raio
    │   ├── candidatura/        # candidaturas, etapas, feedbacks, agendamentos e histórico
    │   ├── chat/               # mensagens por candidatura
    │   ├── notificacoes/       # notificações internas
    │   ├── mensageria/         # filas RabbitMQ e envio de e-mails
    │   ├── storage/            # upload de imagens/arquivos (Cloudflare R2)
    │   ├── geo/                # geocodificação reversa (cidade/estado)
    │   ├── config/             # segurança, WebSocket, RabbitMQ, OAuth e seed de dados
    │   └── exception/          # tratamento global de erros
    ├── main/resources/application.yaml
    └── test/                   # testes unitários dos serviços
```

---

## Como executar

### Pré-requisitos

- JDK 25
- Docker e Docker Compose
- Conta/credenciais dos serviços externos que quiser usar: Resend (e-mails), Cloudflare R2 (imagens), Google e LinkedIn (login social)

### 1. Clonar o repositório

```bash
git clone <url-do-repositorio>
cd <pasta-do-repositorio>/backend
```

### 2. Gerar o par de chaves RSA do JWT

Gere as chaves **fora do projeto** (ou apague os arquivos depois de copiar o conteúdo):

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private.pem
openssl rsa -pubout -in private.pem -out public.pem
```

Para colocar cada chave em **uma única linha** no `.env` (com `\n` no lugar das quebras de linha):

```bash
echo "JWT_PRIVATE_KEY=$(awk 'NF {printf "%s\\n", $0}' private.pem)"
echo "JWT_PUBLIC_KEY=$(awk 'NF {printf "%s\\n", $0}' public.pem)"
```

### 3. Configurar o `.env`

```bash
cp .env.example .env
```

Preencha os valores (veja a [tabela abaixo](#variáveis-de-ambiente)). Para desenvolvimento local com o `docker-compose.yml` do projeto, por exemplo:

```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/selecta
SPRING_DATASOURCE_DB=selecta
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<senha>
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=<senha>
FRONTEND_URL=http://localhost:3000
```

> O Spring lê o `.env` automaticamente (em `./.env` ou `./backend/.env`).

### 4. Subir PostgreSQL e RabbitMQ

```bash
docker compose up -d
```

| Serviço | Porta no host |
|---|---|
| PostgreSQL | `5433` |
| RabbitMQ (AMQP) | `5672` |
| RabbitMQ (painel de gerenciamento) | `15672` |

### 5. Rodar a aplicação

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080` e a documentação em **`http://localhost:8080/swagger-ui.html`**.

### Rodando com Docker

```bash
docker build -t selectaapi .
docker run -p 8080:8080 --env-file .env selectaapi
```

> Dentro do container, `localhost` não aponta para o seu computador. Ajuste os hosts de banco e RabbitMQ conforme a rede dos containers. Em plataformas de deploy, prefira cadastrar as variáveis no painel da plataforma.

### Dados de demonstração

Com `app.seed.enabled: true` (valor atual no `application.yaml`), a aplicação cria empresas, candidatos e vagas de exemplo na inicialização (veja `DevelopmentSeedConfig`). **Desative em produção.**

---

## Variáveis de ambiente

| Variável | Descrição |
|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC do PostgreSQL |
| `SPRING_DATASOURCE_DB` | Nome do banco (usado pelo `docker-compose`) |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Credenciais do banco |
| `SPRING_RABBITMQ_HOST` / `SPRING_RABBITMQ_PORT` | Endereço do RabbitMQ |
| `SPRING_RABBITMQ_USERNAME` / `SPRING_RABBITMQ_PASSWORD` | Credenciais do RabbitMQ |
| `SPRING_RABBITMQ_VIRTUAL_HOST` | Virtual host (padrão `/`) |
| `SPRING_RABBITMQ_SSL_ENABLED` | Ativa SSL no RabbitMQ (padrão `false`) |
| `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` | Par de chaves RSA (PEM) usado para assinar e validar os tokens |
| `API_KEY` | Chave da API do Resend (envio de e-mails) |
| `FRONTEND_URL` | URL do frontend (CORS do WebSocket e redirecionamentos do OAuth) |
| `GOOGLE_CLIENT_ID` / `GOOGLE_SECRET_KEY` | Credenciais do login com Google |
| `LINKEDIN_CLIENT_ID` / `LINKEDIN_CLIENT_SECRET` | Credenciais do login com LinkedIn |
| `R2_ENDPOINT` / `R2_BUCKET` | Endpoint e bucket do Cloudflare R2 |
| `R2_ACCESS_KEY_ID` / `R2_SECRET_ACCESS_KEY` | Credenciais do R2 |
| `R2_PUBLIC_BASE_URL` | URL pública base dos arquivos enviados |

---

## Endpoints principais

A lista completa e interativa está no Swagger UI. Resumo por módulo:

### Autenticação — `/auth`
| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/auth` | Login (retorna o token e grava o cookie `token`) |
| `POST` | `/auth/register-company` | Cadastro de empresa |
| `POST` | `/auth/verify-email` | Confirma a conta com o código recebido por e-mail |
| `POST` | `/auth/resend-code` | Reenvia o código de verificação |

Login social: `/oauth2/authorization/google` e `/oauth2/authorization/linkedin`.

### Usuários — `/users`
`POST /users`, `GET /users`, `GET /users/me`, `GET|PUT|DELETE /users/{id}`

### Vagas — `/vagas`
| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/vagas` | Cria vaga |
| `GET` | `/vagas` | Lista com filtros e paginação |
| `GET` | `/vagas/{id}` | Detalhe da vaga |
| `GET` | `/vagas/empresa/{companyProfileId}` | Vagas de uma empresa |
| `GET` | `/vagas/proximas?latitude=&longitude=&raioKm=` | Vagas dentro de um raio |
| `PUT` / `DELETE` | `/vagas/{id}` | Atualiza / remove |

### Candidato — `/candidate-profile`
`POST|GET|PUT /me`, `PUT /me/avatar`, `PUT /me/resume`, CRUD de `/me/experiences` e `/me/projects`, `GET /{userId}`

### Empresa — `/company-profile`
`GET|PUT /me`, `POST|DELETE /me/logo`, `GET /{userId}`

### Candidaturas (candidato) — `/candidaturas`
| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/candidaturas` | Candidatar-se a uma vaga |
| `GET` | `/candidaturas/me` | Minhas candidaturas |
| `GET` | `/candidaturas/{id}` | Detalhe e histórico |
| `GET` | `/candidaturas/{id}/feedbacks` | Feedbacks recebidos |
| `GET` | `/candidaturas/{id}/agendamentos` | Agendamentos |
| `PATCH` | `/candidaturas/{id}/agendamentos/{agendamentoId}/confirmar` | Confirma agendamento |
| `PATCH` | `/candidaturas/{id}/agendamentos/{agendamentoId}/recusar` | Recusa agendamento |
| `DELETE` | `/candidaturas/{id}` | Cancela a candidatura |

### Candidaturas (empresa) — `/empresa/candidaturas`
| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/empresa/candidaturas` | Lista candidaturas das vagas da empresa |
| `GET` | `/empresa/candidaturas/{id}` | Detalhe |
| `PATCH` | `/empresa/candidaturas/{id}/etapa` | Avança a etapa |
| `PATCH` | `/empresa/candidaturas/{id}/status` | Aprova ou reprova |
| `POST` / `GET` | `/empresa/candidaturas/{id}/feedbacks` | Envia / lista feedbacks |
| `PUT` / `DELETE` | `/empresa/candidaturas/{id}/feedbacks/{feedbackId}` | Edita / remove feedback |
| `POST` / `GET` | `/empresa/candidaturas/{id}/agendamentos` | Cria / lista agendamentos |
| `PUT` / `DELETE` | `/empresa/candidaturas/{id}/agendamentos/{agendamentoId}` | Edita / cancela agendamento |

### Chat — `/message`
`GET /{candidaturaId}` (histórico), `POST /{candidaturaId}` (enviar), `PATCH /{candidaturaId}/lidas` (marcar como lidas)

### Notificações — `/notificacoes`
`GET /notificacoes`, `GET /notificacoes/unreads`, `PATCH /notificacoes` (todas), `PATCH /notificacoes/{notificacaoId}`

---

## Chat em tempo real (WebSocket)

- Endpoint de conexão: `/ws` (origem permitida definida por `FRONTEND_URL`)
- Prefixo de destinos da aplicação: `/app`
- Broker simples em `/queue`, com destinos por usuário em `/user/...`
- A autenticação do WebSocket usa o JWT enviado no cookie `token`

As mensagens enviadas pelo endpoint REST (`POST /message/{candidaturaId}`) são entregues ao destinatário pelo WebSocket e também geram uma notificação.

---

## Testes

Os testes unitários cobrem as camadas de serviço (autenticação, usuários, perfil do candidato e vagas), usando JUnit 5 e Mockito.

```bash
./mvnw test
```

---

## Segurança

- Os tokens JWT são assinados com RSA. As chaves ficam em variáveis de ambiente e **nunca devem ser commitadas**.
- `.env`, `*.pem` e outros arquivos com segredos devem estar no `.gitignore`. Use o `.env.example` apenas como modelo, sem valores reais.
- As rotas públicas são login, cadastro, verificação de e-mail, OAuth2, Swagger e consulta de vagas. As demais exigem autenticação.
- Antes de ir para produção, revise: `ddl-auto: update` (prefira migrations), `show-sql: true`, `app.seed.enabled`, e a configuração de CORS, hoje liberada para qualquer origem.
