-- NUMMIA - esquema inicial do MVP

CREATE TABLE users (
    id                     UUID PRIMARY KEY,
    name                   VARCHAR(120)  NOT NULL,
    email                  VARCHAR(160)  NOT NULL,
    password_hash          VARCHAR(255)  NOT NULL,
    role                   VARCHAR(20)   NOT NULL DEFAULT 'USER',
    currency               VARCHAR(3)    NOT NULL DEFAULT 'BRL',
    monthly_income         NUMERIC(15, 2),
    ai_assistant_enabled   BOOLEAN       NOT NULL DEFAULT TRUE,
    notifications_enabled  BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE accounts (
    id               UUID PRIMARY KEY,
    user_id          UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name             VARCHAR(80)    NOT NULL,
    type             VARCHAR(20)    NOT NULL,
    institution      VARCHAR(80),
    initial_balance  NUMERIC(15, 2) NOT NULL DEFAULT 0,
    credit_limit     NUMERIC(15, 2),
    color            VARCHAR(7),
    archived         BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_accounts_user ON accounts (user_id);

CREATE TABLE categories (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(60)  NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    color       VARCHAR(7),
    icon        VARCHAR(40),
    archived    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_categories_user ON categories (user_id);

CREATE TABLE transactions (
    id                      UUID PRIMARY KEY,
    user_id                 UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    account_id              UUID           NOT NULL REFERENCES accounts (id),
    destination_account_id  UUID REFERENCES accounts (id),
    category_id             UUID REFERENCES categories (id),
    type                    VARCHAR(20)    NOT NULL,
    amount                  NUMERIC(15, 2) NOT NULL,
    description             VARCHAR(160)   NOT NULL,
    transaction_date        DATE           NOT NULL,
    notes                   VARCHAR(500),
    recurring               BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_transactions_amount CHECK (amount > 0),
    CONSTRAINT ck_transactions_type CHECK (type IN ('INCOME', 'EXPENSE', 'TRANSFER'))
);
CREATE INDEX idx_transactions_user_date ON transactions (user_id, transaction_date);
CREATE INDEX idx_transactions_account ON transactions (account_id);
CREATE INDEX idx_transactions_destination ON transactions (destination_account_id);
CREATE INDEX idx_transactions_category ON transactions (category_id);

CREATE TABLE budgets (
    id               UUID PRIMARY KEY,
    user_id          UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id      UUID           NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    budget_year      INTEGER        NOT NULL,
    budget_month     INTEGER        NOT NULL,
    limit_amount     NUMERIC(15, 2) NOT NULL,
    alert_threshold  INTEGER        NOT NULL DEFAULT 80,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_budgets_category_month UNIQUE (user_id, category_id, budget_year, budget_month),
    CONSTRAINT ck_budgets_month CHECK (budget_month BETWEEN 1 AND 12),
    CONSTRAINT ck_budgets_limit CHECK (limit_amount > 0)
);

CREATE TABLE goals (
    id              UUID PRIMARY KEY,
    user_id         UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name            VARCHAR(100)   NOT NULL,
    description     VARCHAR(500),
    target_amount   NUMERIC(15, 2) NOT NULL,
    current_amount  NUMERIC(15, 2) NOT NULL DEFAULT 0,
    deadline        DATE,
    status          VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    icon            VARCHAR(40),
    color           VARCHAR(7),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_goals_target CHECK (target_amount > 0),
    CONSTRAINT ck_goals_current CHECK (current_amount >= 0)
);
CREATE INDEX idx_goals_user ON goals (user_id);

CREATE TABLE goal_contributions (
    id                 UUID PRIMARY KEY,
    goal_id            UUID           NOT NULL REFERENCES goals (id) ON DELETE CASCADE,
    amount             NUMERIC(15, 2) NOT NULL,
    contribution_date  DATE           NOT NULL,
    note               VARCHAR(200),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_goal_contributions_goal ON goal_contributions (goal_id);

CREATE TABLE notifications (
    id             UUID PRIMARY KEY,
    user_id        UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type           VARCHAR(30)   NOT NULL,
    severity       VARCHAR(20)   NOT NULL,
    title          VARCHAR(120)  NOT NULL,
    message        VARCHAR(1000) NOT NULL,
    reference_key  VARCHAR(150)  NOT NULL,
    is_read        BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_notifications_reference UNIQUE (user_id, reference_key)
);
CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at);

CREATE TABLE assistant_messages (
    id          UUID PRIMARY KEY,
    user_id     UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role        VARCHAR(20)   NOT NULL,
    content     VARCHAR(8000) NOT NULL,
    source      VARCHAR(20),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_assistant_messages_user_created ON assistant_messages (user_id, created_at);

-- Auditoria é mantida mesmo após exclusão do usuário (user_id sem FK).
CREATE TABLE audit_logs (
    id           UUID PRIMARY KEY,
    user_id      UUID,
    action       VARCHAR(60)  NOT NULL,
    entity_type  VARCHAR(60),
    entity_id    UUID,
    details      VARCHAR(500),
    ip_address   VARCHAR(64),
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_audit_logs_user ON audit_logs (user_id, created_at);
