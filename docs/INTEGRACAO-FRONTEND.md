# Guia de integração — Front-End ↔ Back-End NUMMIA

Este guia reúne o que o time de Front-End precisa para consumir a API: configuração, autenticação, os CRUDs de cada tela e o tratamento de erros.

**Arquivos prontos para copiar no projeto React:**

| Arquivo | Destino sugerido | Conteúdo |
|---|---|---|
| [`frontend/types.ts`](frontend/types.ts) | `src/api/types.ts` | Tipos TypeScript de todas as requisições e respostas |
| [`frontend/api.ts`](frontend/api.ts) | `src/api/api.ts` | Cliente Axios com token JWT, renovação automática e todas as funções de CRUD |

A documentação interativa (Swagger) fica em **http://localhost:8080/swagger-ui.html**, onde dá para testar qualquer endpoint.

---

## 1. Configuração

1. Suba o Back-End (veja o [README](../README.md)). A API responde em `http://localhost:8080`.
2. No projeto do Front, instale o Axios e crie o `.env`:
   ```bash
   npm install axios
   ```
   ```
   VITE_API_URL=http://localhost:8080
   ```
3. Copie `types.ts` e `api.ts` para `src/api/`.

**CORS:** o Back-End aceita requisições de `http://localhost:5173` (porta padrão do Vite). Se o Front rodar em outra porta ou domínio, ajuste `CORS_ALLOWED_ORIGINS` no `.env` do Back-End (várias origens separadas por vírgula).

---

## 2. Autenticação

```
POST /api/auth/register  →  cria a conta e já devolve os tokens
POST /api/auth/login     →  devolve os tokens
POST /api/auth/refresh   →  troca o refreshToken por um novo par de tokens
POST /api/auth/logout    →  invalida o refreshToken
```

Resposta de login e cadastro:

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "JfajPHs6N_3SPRsL...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": { "id": "...", "name": "Ana", "email": "ana@email.com", "currency": "BRL", ... }
}
```

Como funciona:

- Envie `Authorization: Bearer <accessToken>` em todas as rotas, exceto `/api/auth/*`. O `api.ts` faz isso sozinho.
- O **accessToken dura 15 minutos**. Quando expira, a API responde `401`. O `api.ts` chama `/auth/refresh`, guarda os tokens novos e repete a requisição sem o usuário perceber.
- O **refreshToken dura 30 dias e só pode ser usado uma vez**. Cada refresh devolve um novo. Se um token já usado for reenviado, o Back-End encerra todas as sessões do usuário por segurança. Por isso o `api.ts` garante uma única chamada de refresh por vez.
- Se o refresh falhar, o `api.ts` limpa os tokens e redireciona para `/login`. Para mudar isso, use `setOnSessionExpired(() => navigate('/login'))`.
- Ao cadastrar, o usuário já recebe **14 categorias padrão**, com cor e ícone do Lucide.

Exemplo de uso numa tela de login:

```tsx
import { authApi, apiErrorMessage } from '@/api/api';

async function onSubmit(values: { email: string; password: string }) {
  try {
    const { user } = await authApi.login(values);
    setUser(user);
    navigate('/dashboard');
  } catch (e) {
    toast.error(apiErrorMessage(e)); // "E-mail ou senha inválidos."
  }
}
```

Regras de senha: **8 a 72 caracteres, com letras e números**.

---

## 3. Telas × endpoints

Todos os caminhos abaixo começam com `/api`. Os nomes à direita são as funções do `api.ts`.

### Login e cadastro
| Ação | Endpoint | Função |
|---|---|---|
| Cadastrar | `POST /auth/register` | `authApi.register` |
| Entrar | `POST /auth/login` | `authApi.login` |
| Sair | `POST /auth/logout` | `authApi.logout` |

### Dashboard
| Ação | Endpoint | Função |
|---|---|---|
| Tudo da tela inicial, numa chamada | `GET /dashboard?month=2026-11` | `dashboardApi.get` |

A resposta traz `summary` (saldo total, receitas, despesas, resultado, taxa de poupança, despesas e receitas por categoria), `monthlyEvolution` (últimos 6 meses, pronto para o gráfico de barras do Recharts), `accounts`, `budgets`, `goals` ativas, `recentTransactions` (5 últimas) e `unreadNotifications`. Sem `month`, usa o mês atual.

Para gráficos:
- Pizza ou donut de gastos: `summary.expensesByCategory` (campos `name`, `total`, `percentage`, `color`).
- Barras de receitas × despesas: `monthlyEvolution` (campos `month`, `income`, `expense`).

### Movimentações
| Ação | Endpoint | Função |
|---|---|---|
| Listar com filtros e paginação | `GET /transactions` | `transactionApi.list(filtros)` |
| Detalhe | `GET /transactions/{id}` | `transactionApi.get` |
| Criar | `POST /transactions` | `transactionApi.create` |
| Editar | `PUT /transactions/{id}` | `transactionApi.update` |
| Excluir | `DELETE /transactions/{id}` | `transactionApi.remove` |

Filtros (todos opcionais): `from`, `to` (`yyyy-MM-dd`), `type`, `accountId`, `categoryId`, `minAmount`, `maxAmount`, `recurring`, `search` (busca na descrição), `page` (começa em 0) e `size` (1 a 100, padrão 20). A lista vem da data mais recente para a mais antiga, no formato `{ content, page, size, totalElements, totalPages, last }`.

Corpo para criar ou editar:

```json
// Despesa ou receita: categoryId obrigatório, do mesmo tipo da movimentação
{ "type": "EXPENSE", "amount": 89.90, "description": "Mercado", "date": "2026-11-27",
  "accountId": "<uuid>", "categoryId": "<uuid>", "notes": "opcional", "recurring": false }

// Transferência: sem categoria, com conta de destino diferente da origem
{ "type": "TRANSFER", "amount": 500, "description": "Reserva", "date": "2026-11-27",
  "accountId": "<origem>", "destinationAccountId": "<destino>" }
```

Dicas para o formulário:
- Ao trocar o tipo (receita, despesa ou transferência), recarregue as categorias com `categoryApi.list('INCOME' | 'EXPENSE')` e mostre o campo "conta de destino" só para transferência.
- `amount` é sempre positivo. O sinal vem do `type`. Na listagem, mostre despesas em vermelho com "−" e receitas em verde com "+".

### Contas e cartões
| Ação | Endpoint | Função |
|---|---|---|
| Listar com saldo atual | `GET /accounts?includeArchived=false` | `accountApi.list` |
| Detalhe | `GET /accounts/{id}` | `accountApi.get` |
| Criar | `POST /accounts` | `accountApi.create` |
| Editar | `PUT /accounts/{id}` | `accountApi.update` |
| Excluir | `DELETE /accounts/{id}` | `accountApi.remove` |

- Tipos: `CHECKING` (corrente), `SAVINGS` (poupança), `WALLET` (carteira), `CREDIT_CARD` (cartão), `INVESTMENT` (investimento), `OTHER` (outro).
- `currentBalance` é calculado pelo Back-End. Não calcule no Front.
- Contas com movimentações **não podem ser excluídas** (a API responde `409`). Ofereça "Arquivar", que é um `PUT` com `"archived": true`.

### Categorias e planejamento de gastos
| Ação | Endpoint | Função |
|---|---|---|
| Listar | `GET /categories?type=EXPENSE` | `categoryApi.list` |
| Criar | `POST /categories` | `categoryApi.create` |
| Editar | `PUT /categories/{id}` | `categoryApi.update` |
| Excluir | `DELETE /categories/{id}` | `categoryApi.remove` |
| Orçamentos do mês | `GET /budgets?month=2026-11` | `budgetApi.list` |
| Criar orçamento | `POST /budgets` | `budgetApi.create` |
| Editar orçamento | `PUT /budgets/{id}` | `budgetApi.update` |
| Excluir orçamento | `DELETE /budgets/{id}` | `budgetApi.remove` |
| Copiar orçamentos para outro mês | `POST /budgets/copy` `{ "from": "2026-11", "to": "2026-12" }` | `budgetApi.copy` |
| Sugestões de limite | `GET /analysis/budget-suggestions` | `analysisApi.budgetSuggestions` |

- `icon` é o nome do ícone no Lucide (`utensils`, `car`, `home`...) e `color` vem no formato `#RRGGBB`.
- Categorias com movimentações não podem ser excluídas (`409`). Use `archived: true`.
- Orçamento só vale para categoria de **despesa**, e é um por categoria por mês.
- Cada orçamento já vem com `spent`, `remaining`, `usedPercentage` e `status`. Sugestão de cores para a barra de progresso: `OK` verde, `WARNING` amarelo, `EXCEEDED` vermelho.
- As sugestões de limite são a média dos últimos 3 meses menos 10%. Dá para mostrar um botão "Aplicar sugestão" que chama `budgetApi.create`.

### Metas financeiras
| Ação | Endpoint | Função |
|---|---|---|
| Listar | `GET /goals?status=ACTIVE` | `goalApi.list` |
| Criar | `POST /goals` | `goalApi.create` |
| Editar ou cancelar | `PUT /goals/{id}` (com `"status": "CANCELLED"` para cancelar) | `goalApi.update` |
| Excluir | `DELETE /goals/{id}` | `goalApi.remove` |
| Guardar ou resgatar dinheiro | `POST /goals/{id}/contributions` `{ "amount": 200 }` (valor negativo = resgate) | `goalApi.contribute` |
| Histórico de aportes | `GET /goals/{id}/contributions` | `goalApi.contributions` |

A resposta traz `progressPercentage` e `remainingAmount`. Quando há prazo, traz também `monthlyAmountNeeded`, para mostrar algo como "guarde R$ X por mês". A meta vira `COMPLETED` sozinha ao atingir o valor.

### Assistente de IA
| Ação | Endpoint | Função |
|---|---|---|
| Enviar pergunta | `POST /assistant/chat` `{ "message": "..." }` | `assistantApi.chat` |
| Histórico, das mais antigas para as mais novas | `GET /assistant/messages?limit=50` | `assistantApi.messages` |
| Limpar conversa | `DELETE /assistant/messages` | `assistantApi.clear` |

- A resposta traz `reply`, que é texto com quebras de linha `\n` e marcadores "•" (renderize com `white-space: pre-line`), e `suggestions`, que podem virar botões de perguntas rápidas.
- Enquanto o serviço de IA do outro projeto não estiver ligado, as respostas vêm de regras locais (`source: "LOCAL"`).
- Se o usuário desativou o assistente nas configurações, a API responde `422`.

### Notificações
| Ação | Endpoint | Função |
|---|---|---|
| Listar | `GET /notifications?unreadOnly=false&page=0&size=20` | `notificationApi.list` |
| Contador para o sino | `GET /notifications/unread-count` | `notificationApi.unreadCount` |
| Marcar como lida | `PATCH /notifications/{id}/read` | `notificationApi.markRead` |
| Marcar todas | `PATCH /notifications/read-all` | `notificationApi.markAllRead` |
| Excluir | `DELETE /notifications/{id}` | `notificationApi.remove` |

Os alertas são gerados pelo Back-End: orçamento perto do limite ou estourado, meta quase atingida ou concluída, despesa recorrente chegando e resumo mensal. Não há push em tempo real por enquanto. Atualize o contador ao trocar de tela ou a cada 1 a 2 minutos.

### Configurações e perfil
| Ação | Endpoint | Função |
|---|---|---|
| Meus dados | `GET /users/me` | `userApi.me` |
| Atualizar perfil e preferências | `PUT /users/me` | `userApi.update` |
| Trocar senha (encerra as outras sessões) | `PUT /users/me/password` | `userApi.changePassword` |
| Excluir conta e todos os dados (LGPD) | `DELETE /users/me` `{ "password": "..." }` | `userApi.deleteAccount` |

`PUT /users/me` aceita `name` (obrigatório), `currency`, `monthlyIncome`, `aiAssistantEnabled` e `notificationsEnabled`.

### Análises (se quiser uma tela de relatórios)
`GET /analysis/summary`, `/analysis/evolution?months=12`, `/analysis/comparison` (mês atual × anterior por categoria), `/analysis/anomalies` (gastos fora do padrão) e `/analysis/recommendations` (dicas com `severity` `INFO`, `SUCCESS`, `WARNING` ou `CRITICAL`).

---

## 4. Erros

Todos os erros seguem o mesmo formato:

```json
{
  "status": 400,
  "title": "Dados inválidos",
  "detail": "Um ou mais campos são inválidos.",
  "errors": { "email": "deve ser um endereço de e-mail bem formado", "password": "não deve estar em branco" }
}
```

| Status | Quando | O que fazer no Front |
|---|---|---|
| `400` | Campo inválido ou faltando | Mostrar `errors[campo]` embaixo de cada input |
| `401` | Token ausente ou expirado, ou login inválido | `api.ts` renova o token sozinho; no login, mostrar `detail` |
| `404` | Recurso não existe ou é de outro usuário | Mensagem "não encontrado" e voltar para a lista |
| `409` | Duplicado (e-mail, nome de conta ou categoria, orçamento do mês) ou exclusão de item em uso | Mostrar `detail` (sugerir arquivar) |
| `422` | Regra de negócio, por exemplo categoria de tipo errado ou resgate maior que o saldo da meta | Mostrar `detail` |
| `500` | Erro inesperado | Mensagem genérica |

`apiErrorMessage(erro)` no `api.ts` já devolve o texto certo em português.

---

## 5. Formatos e convenções

- **Dinheiro** vem como número (`1250.5`). Formate no Front:
  ```ts
  const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
  brl.format(1250.5); // "R$ 1.250,50"
  ```
- **Datas** vêm como `"2026-11-27"`. Para exibir, evite `new Date("2026-11-27")`, que é lido como UTC e pode mostrar o dia anterior no Brasil. Use `date-fns` (`parseISO` + `format(..., 'dd/MM/yyyy')`) ou separe a string.
- **Mês de referência** vem como `"2026-11"`.
- **Percentuais** vêm de 0 a 100 (`85.5` = 85,5%).
- **Campos vazios não aparecem no JSON.** Por isso vários campos estão como opcionais (`?`) nos tipos.
- Todos os IDs são UUID.
- **`PUT` substitui o registro inteiro.** Na edição, envie todos os campos do formulário, não só os alterados. Um campo omitido vira vazio. Por exemplo, sem `initialBalance` o saldo inicial da conta volta a 0.

---

## 6. Ordem sugerida de implementação

1. Configurar o `api.ts` e as telas de **login e cadastro**, com rota protegida que redireciona para `/login` sem token.
2. **Contas** (CRUD), pois são necessárias para lançar movimentações.
3. **Movimentações** (lista com filtros, paginação e formulário).
4. **Dashboard**, já com dados reais.
5. **Categorias e orçamentos**.
6. **Metas**.
7. **Notificações** (sino com contador) e **Assistente**.
8. **Configurações e perfil**.

Para criar dados de teste rapidamente, use o Swagger: cadastre um usuário, clique em **Authorize**, cole o `accessToken` e chame os `POST`.
