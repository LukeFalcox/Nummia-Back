# NUMMIA — Back-End

API REST da **NUMMIA**, plataforma de gestão financeira pessoal com automações e agentes de Inteligência Artificial.

| Tecnologia | Uso |
|---|---|
| Java 21 + Spring Boot 4 | APIs REST |
| Spring Security + JWT (HS256) | Autenticação stateless com access/refresh token |
| PostgreSQL + Flyway | Banco de dados e versionamento do schema |
| springdoc-openapi | Swagger UI em `/swagger-ui.html` |
| JUnit 5 + MockMvc + H2 | Testes automatizados |
| Docker / GitHub Actions | Ambiente padronizado e CI |

## Como rodar

### Opção 1 — Docker (tudo junto)

```bash
cp .env.example .env        # preencha JWT_SECRET (openssl rand -base64 48)
docker compose up --build   # Postgres + API em http://localhost:8080
docker compose --profile automation up   # inclui o n8n em http://localhost:5678
```

### Opção 2 — Local (para desenvolver)

```bash
docker compose up -d postgres   # ou um PostgreSQL local com banco/usuário/senha "nummia"
./mvnw spring-boot:run          # perfil "dev" por padrão
```

- Swagger: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health
- Testes: `./mvnw test` (usa H2 em memória, não precisa de banco)

### Variáveis de ambiente

| Variável | Descrição | Padrão |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexão PostgreSQL | `localhost:5432/nummia`, `nummia`/`nummia` |
| `JWT_SECRET` | Segredo HS256 (mín. 32 caracteres). **Obrigatório fora do perfil dev** | — |
| `JWT_ACCESS_TTL` / `JWT_REFRESH_TTL` | Validade dos tokens | `15m` / `30d` |
| `CORS_ALLOWED_ORIGINS` | Origens do Front-End (separadas por vírgula) | `http://localhost:5173` |
| `AI_WEBHOOK_URL`, `AI_API_KEY`, `AI_TIMEOUT` | Serviço de IA (n8n/LangGraph/Python) | vazio = assistente local |
| `AUTOMATION_WEBHOOK_URL`, `AUTOMATION_WEBHOOK_SECRET` | Webhook de eventos para o n8n | vazio = desativado |
| `SWAGGER_ENABLED` | Habilita Swagger no perfil `prod` | `false` |

## Arquitetura

Organização **por funcionalidade** (`com.devfalco.nummia.<módulo>`), cada módulo com entidade, repositório, serviço, controller e DTOs:

```
auth/          cadastro, login, refresh token com rotação, logout
user/          perfil, preferências, troca de senha, exclusão de conta (LGPD)
account/       contas financeiras e cartões, saldo calculado
category/      categorias de receita/despesa (14 padrão criadas no cadastro)
transaction/   receitas, despesas e transferências, filtros e paginação
budget/        orçamento mensal por categoria (OK / WARNING / EXCEEDED)
goal/          metas financeiras, aportes/resgates, valor mensal necessário
dashboard/     indicadores consolidados para a tela inicial
analysis/      Agente de Análise e Planejamento (regras de negócio)
assistant/     Agente Financeiro (chat) + integração com o serviço de IA
notification/  Agente de Notificações (alertas, lembretes, resumo mensal)
automation/    envio de eventos assinados (HMAC) para o n8n
audit/         registro de operações relevantes
```

Princípios do planejamento aplicados:

- **Isolamento por usuário:** toda consulta é filtrada pelo `sub` do JWT; acessar dados de outro usuário retorna `404`.
- **Cálculos no Back-End, interpretação na IA:** saldos, comparações, anomalias e recomendações são calculados em `AnalysisService`. A IA recebe apenas o resultado.
- **IA sem acesso ao banco e somente leitura:** o assistente não cria, altera nem exclui dados financeiros.
- **Minimização de dados (LGPD):** o contexto enviado à IA não contém nome nem e-mail; `GET /api/assistant/context` mostra exatamente o que é compartilhado. `DELETE /api/users/me` apaga a conta e todos os dados.
- **Segurança:** BCrypt, refresh tokens opacos armazenados como hash SHA-256 com rotação e detecção de reuso, validação de entrada, erros padronizados (RFC 7807 `ProblemDetail`), segredos apenas por variável de ambiente.

## Endpoints

Todos sob `/api`, autenticados com `Authorization: Bearer <accessToken>` (exceto `/api/auth/**`). Documentação completa no Swagger.

| Recurso | Endpoints |
|---|---|
| Autenticação | `POST /auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout` |
| Usuário | `GET/PUT/DELETE /users/me`, `PUT /users/me/password` |
| Contas | `GET/POST /accounts`, `GET/PUT/DELETE /accounts/{id}` |
| Categorias | `GET/POST /categories?type=`, `GET/PUT/DELETE /categories/{id}` |
| Movimentações | `GET /transactions?from&to&type&accountId&categoryId&minAmount&maxAmount&recurring&search&page&size`, `POST`, `GET/PUT/DELETE /transactions/{id}` |
| Orçamentos | `GET /budgets?month=yyyy-MM`, `POST`, `GET/PUT/DELETE /budgets/{id}`, `POST /budgets/copy` |
| Metas | `GET /goals?status=`, `POST`, `GET/PUT/DELETE /goals/{id}`, `GET/POST /goals/{id}/contributions` |
| Dashboard | `GET /dashboard?month=yyyy-MM` |
| Análises | `GET /analysis/summary`, `/evolution`, `/comparison`, `/anomalies`, `/recommendations`, `/budget-suggestions` |
| Assistente | `POST /assistant/chat`, `GET/DELETE /assistant/messages`, `GET /assistant/context` |
| Notificações | `GET /notifications`, `GET /notifications/unread-count`, `PATCH /notifications/{id}/read`, `PATCH /notifications/read-all`, `DELETE /notifications/{id}` |

Exemplo rápido:

```bash
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"name":"Ana","email":"ana@email.com","password":"senha1234"}'
```

## Integração com a equipe de IA e Automações

### Assistente (`AI_WEBHOOK_URL`)

A cada mensagem, o Back-End faz `POST` no webhook configurado com:

```json
{
  "sessionId": "uuid-do-usuario",
  "message": "Quanto gastei com alimentação?",
  "history": [{ "role": "user", "content": "..." }, { "role": "assistant", "content": "..." }],
  "context": {
    "today": "2026-11-26", "currency": "BRL",
    "summary": { "income": 5000.00, "expense": 450.00, "expensesByCategory": [ ... ] },
    "comparison": { ... }, "accounts": [ ... ], "budgets": [ ... ], "goals": [ ... ],
    "anomalies": [ ... ], "recommendations": [ ... ]
  }
}
```

Resposta esperada: `{"reply": "texto", "suggestions": ["..."]}` (também aceita `{"output": "..."}`, padrão do AI Agent do n8n). Se o webhook não estiver configurado ou falhar, o **assistente local por regras** responde (saldo, gastos, receitas, orçamentos, metas, comparação e dicas). Header `Authorization: Bearer $AI_API_KEY` é enviado quando configurado.

### Eventos para automação (`AUTOMATION_WEBHOOK_URL`)

Quando uma notificação é gerada, o Back-End envia:

```json
{ "event": "notification.created", "userId": "...", "occurredAt": "...",
  "data": { "notificationId": "...", "type": "BUDGET_EXCEEDED", "severity": "CRITICAL", "title": "...", "message": "..." } }
```

Headers: `X-Nummia-Event` e, se `AUTOMATION_WEBHOOK_SECRET` estiver definido, `X-Nummia-Signature: sha256=<HMAC-SHA256 do corpo>` para o n8n validar a origem.

### Notificações automáticas

| Tipo | Quando |
|---|---|
| `BUDGET_WARNING` / `BUDGET_EXCEEDED` | Ao lançar despesa que atinge o limite de alerta (padrão 80%) ou ultrapassa 100% do orçamento |
| `GOAL_NEAR` / `GOAL_COMPLETED` | Ao fazer aporte que leva a meta a 90% ou 100% |
| `RECURRING_EXPENSE` | Diariamente às 09h, para despesas recorrentes que vencem nos próximos 3 dias |
| `MONTHLY_SUMMARY` | Todo dia 1º às 08h, resumo do mês anterior |

Cada evento gera no máximo uma notificação (deduplicação por chave de referência) e respeita a preferência `notificationsEnabled` do usuário.

## Próximos passos sugeridos

- Rate limiting no login e recuperação de senha por e-mail.
- Exportação dos dados do usuário (portabilidade - LGPD).
- Testes de integração com Testcontainers (PostgreSQL real) no CI.
- Controle de faturas de cartão, importação de extratos (OFX/CSV) e Open Finance.
