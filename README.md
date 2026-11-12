# Selecta API — Processos Seletivos Mais Transparentes e Humanizados

API REST para um processo seletivo em que o candidato **acompanha cada etapa da candidatura**, recebe **feedbacks**, **agenda entrevistas** e conversa com a empresa em **tempo real**, em vez de ficar sem resposta depois de enviar o currículo.

O repositório contém o **backend** (`backend/`), construído com Spring Boot.

---

## Sumário

- [Funcionalidades](#funcionalidades)
- [Como funciona o processo seletivo](#como-funciona-o-processo-seletivo)
- [Recomendação personalizada de vagas](#recomendação-personalizada-de-vagas)
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
- Cadastro e login (e-mail/senha com verificação por código, ou OAuth2 com **Google** e **LinkedIn**), com renovação de sessão por *refresh token*
- Perfil completo: dados pessoais, experiências, projetos, avatar e currículo em PDF (armazenado em bucket privado)
- Busca de vagas com filtros (título, categoria, modalidade, faixa salarial, cidade, estado) e **busca por proximidade** (raio em km)
- **Vagas recomendadas** de acordo com o perfil, que podem ser desativadas nas preferências
- Candidatura a vagas e acompanhamento da etapa e do status do processo
- Visualização de feedbacks e histórico da candidatura
- Confirmação ou recusa de agendamentos de entrevista
- Chat com a empresa dentro de cada candidatura
- Notificações
- **Preferências** de privacidade do perfil (e-mail, telefone, currículo e foto visíveis para a empresa) e de e-mails recebidos

**Empresas**
- Cadastro de empresa e perfil com logo
- Criação e gerenciamento de vagas, com **etapas configuráveis** do processo
- **Etapas personalizadas**: a empresa cria, edita, habilita/desabilita e remove as próprias etapas, além de usar as etapas padrão do sistema
- **Equipe da empresa**: o dono cadastra membros com papel `ADMIN` ou `RECRUTADOR`
- Painel de candidaturas com filtros (vaga, status, etapa e nome do candidato): avançar etapa, aprovar ou reprovar
- Envio de feedbacks aos candidatos
- Agendamento de entrevistas e testes
- Chat com candidatos

**Plataforma**
- E-mails transacionais assíncronos (confirmação de conta e atualizações da candidatura) via fila, respeitando as preferências do usuário
- Notificações internas e mensagens em tempo real
- Índice de recomendação atualizado em segundo plano (filas + reconciliação periódica)
- Documentação interativa com Swagger UI

---

## Como funciona o processo seletivo

Cada candidatura tem uma **etapa** (onde o candidato está) e um **status** (como ele está).

As etapas são cadastros (`tipos_de_etapas`), e não mais uma lista fixa no código. Existem as **etapas do sistema**, comuns a todas as empresas e somente leitura, e as **etapas personalizadas**, criadas por cada empresa.

**Etapas do sistema** (criadas na inicialização por `db/init.sql`):

| Papel na vaga | Código | Descrição |
|---|---|---|
| Inicial (automática) | `INSCRICAO` | Candidatura recebida |
| Intermediária | `TRIAGEM` | Análise inicial dos candidatos |
| Intermediária | `ENTREVISTA_RH` | Entrevista com RH |
| Intermediária | `TESTE_TECNICO` | Teste técnico |
| Intermediária | `ENTREVISTA_TECNICA` | Entrevista técnica |
| Intermediária | `PROPOSTA` | Proposta |
| Final (automática) | `ENCERRADA` | Processo seletivo finalizado |

Ao criar uma vaga, a empresa escolhe em `etapasId` quais etapas intermediárias usar, na ordem desejada. Podem ser etapas do sistema ou personalizadas da própria empresa, sem repetição e no máximo 20. `INSCRICAO` e `ENCERRADA` são adicionadas automaticamente no começo e no fim. Se nenhuma etapa for informada, vale o padrão: `TRIAGEM → ENTREVISTA_RH → TESTE_TECNICO → ENTREVISTA_TECNICA → PROPOSTA`.

A empresa só avança a candidatura para a **próxima etapa configurada na vaga** (o `PATCH` recebe o `etapaId` esperado). Ao **aprovar**, a candidatura vai para `ENCERRADA` e consome uma das posições da vaga (`quantidadeVagas`). Quando as posições acabam, a vaga é desativada automaticamente. Ao **reprovar**, a candidatura é finalizada na etapa em que estava.

**Etapas personalizadas:** têm nome único por empresa, podem ser desabilitadas e só podem ser removidas se ainda não tiverem sido usadas em uma vaga.

**Status da candidatura:** `EM_ANDAMENTO`, `APROVADA`, `REPROVADA`, `CANCELADA` (pelo candidato).

**Status do agendamento:** `PENDENTE`, `CONFIRMADO`, `RECUSADO`, `CANCELADO`, `REALIZADO`, `NAO_COMPARECEU`.

**Modalidades de vaga:** `PRESENCIAL`, `REMOTO`, `HIBRIDO`.

**Categorias de vaga (`VagaRole`):** 93 áreas, como `DESENVOLVIMENTO_SOFTWARE`, `VENDAS`, `FINANCEIRO`, `ENGENHARIA_CIVIL` e `OUTRO`. A lista completa está no enum e no Swagger.

**Salário:** a vaga pode informar um valor ou marcar `salarioACombinar`.

**Perfis de usuário:** `CANDIDATE`, `COMPANY`, `ADMIN`.

**Papéis na equipe da empresa:** o dono da empresa, `ADMIN` e `RECRUTADOR`. Somente o dono cria ou remove administradores e altera papéis.

**Tipos de notificação:** `AGENDAMENTO`, `CANDIDATURA_ATUALIZACAO`, `CHAT`, `VAGAS`.

---

## Recomendação personalizada de vagas

Quando um candidato autenticado lista as vagas, elas podem ser ordenadas por **similaridade com o perfil dele**:

1. O texto do perfil (objetivo, resumo, formação, habilidades, experiências, projetos e localização) e o da vaga viram **embeddings** pela API de embeddings da OpenAI (`text-embedding-3-small`, 1536 dimensões).
2. Os vetores ficam no PostgreSQL com a extensão **pgvector** (tabelas `candidate_embeddings` e `job_embeddings`, com índice HNSW).
3. A listagem ordena por similaridade de cosseno, com um pequeno bônus para vagas não remotas na mesma cidade ou no mesmo estado do candidato. Os filtros normais continuam valendo.
4. A resposta de `GET /vagas` traz o header **`X-Recomendacao`** com o valor `personalizada` ou `padrao`.

Os embeddings são gerados de forma assíncrona (fila `embedding.atualizacao`, com *dead letter*) quando o perfil ou a vaga mudam. Um job agendado (`EmbeddingReconciler`) reenfileira itens sem embedding, desatualizados ou gerados por outro modelo.

**É opcional.** Sem `OPENAI_API_KEY`, ou se o pgvector não estiver disponível, a API continua funcionando e a listagem usa a ordenação padrão. O candidato também pode desativar a personalização em `PUT /users/me/preferences` (`personalizedJobs`), o que apaga o embedding dele.

---

## Tecnologias

| Área | Tecnologia |
|---|---|
| Linguagem | Java 25 |
| Framework | Spring Boot 4.1.0 (Web MVC, Data JPA, Validation, Scheduling) |
| Segurança | Spring Security, JWT assinado com RSA, OAuth2 Login (Google e LinkedIn) |
| Banco de dados | PostgreSQL 16 com **pgvector** |
| Mensageria | RabbitMQ 4 (com filas *dead letter* e retry) |
| Tempo real | WebSocket / STOMP |
| Recomendação | Embeddings da OpenAI + busca vetorial (pgvector) |
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
├── docker-compose.yml          # PostgreSQL (pgvector) + RabbitMQ para desenvolvimento
├── .env.example                # modelo das variáveis de ambiente
├── pom.xml
└── src/
    ├── main/java/com/unisal/selectaapi/
    │   ├── auth/               # login, refresh token, cadastro de empresa, verificação de e-mail, OAuth2
    │   ├── user/               # usuários, perfis de acesso e preferências
    │   ├── candidate/          # perfil do candidato, experiências, projetos, avatar e currículo
    │   ├── company/            # perfil da empresa, logo e equipe (membros)
    │   ├── vagas/              # vagas, etapas (sistema e personalizadas), filtros e busca por raio
    │   ├── candidatura/        # candidaturas, etapas, feedbacks, agendamentos e histórico
    │   ├── recomendacao/       # embeddings, indexação assíncrona e ranking de vagas
    │   ├── chat/               # mensagens por candidatura
    │   ├── notificacoes/       # notificações internas
    │   ├── mensageria/         # filas RabbitMQ e envio de e-mails
    │   ├── storage/            # upload de imagens e currículos (Cloudflare R2)
    │   ├── geo/                # geocodificação reversa (cidade/estado)
    │   ├── config/             # segurança, WebSocket, RabbitMQ, agendamento, OAuth e seed de dados
    │   └── exception/          # tratamento global de erros
    ├── main/resources/
    │   ├── application.yaml
    │   └── db/init.sql         # índice e etapas padrão do sistema
    └── test/                   # testes unitários dos serviços
```

---

## Como executar

### Pré-requisitos

- JDK 25
- Docker e Docker Compose
- Conta/credenciais dos serviços externos que quiser usar: Resend (e-mails), Cloudflare R2 (imagens e currículos), Google e LinkedIn (login social) e OpenAI (recomendação de vagas)

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

O PostgreSQL usa a imagem `pgvector/pgvector:pg16`, que já inclui a extensão usada na recomendação de vagas. Se você usar outro Postgres, instale o pgvector nele. Sem a extensão, a API sobe normalmente e apenas a recomendação fica desativada.

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

Com `app.seed.enabled: true` (valor atual no `application.yaml`), a aplicação cria as etapas do sistema, empresas, candidatos e vagas de exemplo na inicialização (veja `DevelopmentSeedConfig`). **Desative em produção.**

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
| `FRONTEND_URL` | URL do frontend (redirecionamentos do OAuth; padrão `http://localhost:3000`) |
| `GOOGLE_CLIENT_ID` / `GOOGLE_SECRET_KEY` | Credenciais do login com Google |
| `LINKEDIN_CLIENT_ID` / `LINKEDIN_CLIENT_SECRET` | Credenciais do login com LinkedIn |
| `R2_ENDPOINT` | Endpoint do Cloudflare R2 |
| `R2_BUCKET` | Bucket das imagens (avatar e logo) |
| `R2_RESUME_BUCKET` | Bucket **privado**, sem domínio público, para os PDFs de currículos |
| `R2_ACCESS_KEY_ID` / `R2_SECRET_ACCESS_KEY` | Credenciais do R2 |
| `R2_PUBLIC_BASE_URL` | URL pública base dos arquivos enviados |
| `OPENAI_API_KEY` | *(opcional)* Chave da OpenAI. Sem ela, a recomendação personalizada fica desativada |
| `OPENAI_EMBEDDING_MODEL` | *(opcional)* Modelo de embeddings (padrão `text-embedding-3-small`) |
| `OPENAI_BASE_URL` | *(opcional)* URL base da API compatível com OpenAI (padrão `https://api.openai.com/v1`) |

---

## Endpoints principais

A lista completa e interativa está no Swagger UI. Resumo por módulo:

### Autenticação — `/auth`
| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/auth` | Login (retorna o token e grava os cookies `token` e `refresh_token`) |
| `POST` | `/auth/refresh-token` | Renova o JWT com o refresh token (o token é rotacionado) |
| `POST` | `/auth/register-company` | Cadastro de empresa |
| `POST` | `/auth/verify-email` | Confirma a conta com o código recebido por e-mail |
| `POST` | `/auth/resend-code` | Reenvia o código de verificação |

Login social: `/oauth2/authorization/google` e `/oauth2/authorization/linkedin`.

### Usuários — `/users`
`POST /users`, `GET /users` (apenas `ADMIN`), `GET /users/me`, `GET|PUT|DELETE /users/{id}` (o próprio usuário ou `ADMIN`)

### Preferências — `/users/me/preferences`
`GET` e `PUT`. Campos: `showEmail`, `showPhone`, `showResume`, `showPhoto`, `emailStageChanges`, `emailDecisions`, `emailFeedback`, `emailAppointments`, `personalizedJobs`.

### Vagas — `/vagas`
| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/vagas` | Cria vaga |
| `GET` | `/vagas` | Lista com filtros e paginação (`page`, padrão 0; `size`, padrão 12 e máximo 50). Ordenação personalizada para candidatos autenticados (header `X-Recomendacao`) |
| `GET` | `/vagas/{id}` | Detalhe da vaga, com as etapas do processo |
| `GET` | `/vagas/empresa/{companyProfileId}` | Vagas de uma empresa |
| `GET` | `/vagas/proximas?latitude=&longitude=&raioKm=` | Vagas dentro de um raio |
| `PUT` / `DELETE` | `/vagas/{id}` | Atualiza / remove |

Filtros de `GET /vagas`: `titulo`, `categoria`, `modalidade`, `salarioMin`, `salarioMax`, `cidade`, `estado`, `ativa` e `companyProfileId`.

### Etapas do processo — `/etapas` (`COMPANY`)
| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/etapas` | Cria etapa personalizada |
| `GET` | `/etapas` | Etapas disponíveis para a empresa (sistema + próprias) |
| `GET` | `/etapas/company` | Apenas as etapas da empresa |
| `GET` | `/etapas/default` | Etapas do sistema |
| `GET` | `/etapas/{id}` | Detalhe |
| `PUT` | `/etapas/{id}` | Edita nome e descrição |
| `PUT` | `/etapas/state/{id}` | Habilita ou desabilita |
| `DELETE` | `/etapas/{id}` | Remove (somente se ainda não utilizada) |

### Candidato — `/candidate-profile`
`POST|GET|PUT /me`, `PUT /me/avatar`, `PUT /me/resume` (PDF de até 10 MB), CRUD de `/me/experiences` e `/me/projects`, `GET /{userId}` e `GET /{userId}/resume/{fileId}.pdf`

O perfil de outro candidato só pode ser visto por uma empresa que tenha recebido candidatura dele, e respeita as preferências de privacidade.

### Empresa — `/company-profile`
`GET|PUT /me`, `POST|DELETE /me/logo`, `GET /{userId}`

### Equipe da empresa — `/empresa/equipe`
| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/empresa/equipe` | Lista os membros da empresa |
| `POST` | `/empresa/equipe` | Cadastra um membro (`ADMIN` ou `RECRUTADOR`) |
| `PATCH` | `/empresa/equipe/{userId}` | Atualiza nome, senha ou papel |
| `DELETE` | `/empresa/equipe/{userId}` | Remove o membro |

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
| `GET` | `/empresa/candidaturas` | Lista candidaturas das vagas da empresa (filtros `vagaId`, `status`, `etapaId`, `candidatoNome`) |
| `GET` | `/empresa/candidaturas/{id}` | Detalhe |
| `PATCH` | `/empresa/candidaturas/{id}/etapa` | Avança para a próxima etapa da vaga (`etapaId` e `observacao`) |
| `PATCH` | `/empresa/candidaturas/{id}/status` | Aprova ou reprova (`status` e `motivo`) |
| `POST` / `GET` | `/empresa/candidaturas/{id}/feedbacks` | Envia / lista feedbacks |
| `PUT` / `DELETE` | `/empresa/candidaturas/{id}/feedbacks/{feedbackId}` | Edita / remove feedback |
| `POST` / `GET` | `/empresa/candidaturas/{id}/agendamentos` | Cria / lista agendamentos |
| `PUT` / `DELETE` | `/empresa/candidaturas/{id}/agendamentos/{agendamentoId}` | Edita / cancela agendamento |

### Chat — `/message`
`GET /{candidaturaId}` (histórico), `POST /{candidaturaId}` (enviar), `PATCH /{candidaturaId}/lidas` (marcar como lidas)

### Notificações — `/notificacoes`
`GET /notificacoes` (não lidas, paginadas), `GET /notificacoes/unreads` (contagem), `PATCH /notificacoes` (todas), `PATCH /notificacoes/{notificacaoId}`

---

## Chat em tempo real (WebSocket)

- Endpoint de conexão: `/ws`
- Prefixo de destinos da aplicação: `/app`
- Broker simples em `/queue`, com destinos por usuário em `/user/...`
- A autenticação do WebSocket usa o JWT enviado no cookie `token`

Destinos que o cliente pode assinar:

| Destino | Conteúdo |
|---|---|
| `/user/queue/mensagens` | Novas mensagens do chat |
| `/user/queue/leituras` | Aviso de que as mensagens foram lidas |
| `/user/queue/notificacoes` | Novas notificações |

As mensagens enviadas pelo endpoint REST (`POST /message/{candidaturaId}`) são entregues ao destinatário pelo WebSocket e também geram uma notificação.

---

## Testes

Os testes unitários cobrem as camadas de serviço (autenticação, usuários, preferências, perfil do candidato, perfil da empresa e vagas, incluindo a regra de salário), usando JUnit 5 e Mockito.

```bash
./mvnw test
```

---

## Segurança

- Os tokens JWT são assinados com RSA. As chaves ficam em variáveis de ambiente e **nunca devem ser commitadas**.
- Os *refresh tokens* são rotacionados a cada uso. Reutilizar um token já revogado invalida todos os tokens do usuário.
- Os currículos ficam em um bucket privado e só são baixados pelo endpoint autenticado, pelo próprio candidato ou por uma empresa que recebeu a candidatura dele.
- `.env`, `*.pem` e outros arquivos com segredos devem estar no `.gitignore`. Use o `.env.example` apenas como modelo, sem valores reais.
- As rotas públicas são login, renovação de token, cadastro, verificação de e-mail, OAuth2, Swagger e `/vagas/**`. As demais exigem autenticação.
- Antes de ir para produção, revise: `ddl-auto: update` (prefira migrations), `show-sql: true`, o log em nível `TRACE` do WebSocket, `app.seed.enabled`, e a configuração de CORS e de origens do WebSocket, hoje liberadas para qualquer origem.
