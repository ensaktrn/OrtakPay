-- Table names are plural ("users", "groups") because "user" and "group" are
-- reserved keywords in PostgreSQL and would otherwise require quoting everywhere.

CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE groups (
    id         UUID         PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    created_by UUID         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,
    -- ON DELETE NO ACTION (default) is intentional: financial records
    -- must never be silently cascade-deleted. Account deactivation will
    -- be handled via a soft-delete flag in a later phase, not DB cascade.
    CONSTRAINT fk_groups_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE group_members (
    id        UUID        PRIMARY KEY,
    group_id  UUID        NOT NULL,
    user_id   UUID        NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_group_members_group FOREIGN KEY (group_id) REFERENCES groups (id),
    CONSTRAINT fk_group_members_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_group_members_group_user UNIQUE (group_id, user_id)
);

-- Composite unique index above already serves "members of a group" (group_id is
-- the leading column); a user's own memberships need their own index since
-- user_id is not a leading column there.
CREATE INDEX idx_group_members_user_id ON group_members (user_id);

CREATE TABLE expenses (
    id          UUID          PRIMARY KEY,
    group_id    UUID          NOT NULL,
    paid_by     UUID          NOT NULL,
    amount      NUMERIC(19,2) NOT NULL,
    description VARCHAR(500),
    split_type  VARCHAR(20)   NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,
    CONSTRAINT fk_expenses_group FOREIGN KEY (group_id) REFERENCES groups (id),
    CONSTRAINT fk_expenses_paid_by FOREIGN KEY (paid_by) REFERENCES users (id),
    CONSTRAINT ck_expenses_amount_positive CHECK (amount > 0)
);

-- "List expenses in a group" is the core, constant read pattern for this table;
-- without this index it would be a full table scan.
CREATE INDEX idx_expenses_group_id ON expenses (group_id);

CREATE TABLE expense_shares (
    id          UUID          PRIMARY KEY,
    expense_id  UUID          NOT NULL,
    user_id     UUID          NOT NULL,
    owed_amount NUMERIC(19,2) NOT NULL,
    CONSTRAINT fk_expense_shares_expense FOREIGN KEY (expense_id) REFERENCES expenses (id),
    CONSTRAINT fk_expense_shares_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_expense_shares_expense_user UNIQUE (expense_id, user_id),
    CONSTRAINT ck_expense_shares_owed_amount_non_negative CHECK (owed_amount >= 0)
);

-- Mirrors idx_group_members_user_id: "how much does this user owe across all
-- expenses" filters by user_id alone, which the composite unique index above
-- (leading column expense_id) does not serve.
CREATE INDEX idx_expense_shares_user_id ON expense_shares (user_id);

-- No CHECK on net_amount: it is a signed running balance (negative means the
-- user owes into the group, positive means the group owes them), so it must
-- be allowed to go negative.
CREATE TABLE balances (
    id         UUID          PRIMARY KEY,
    group_id   UUID          NOT NULL,
    user_id    UUID          NOT NULL,
    net_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    version    BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT fk_balances_group FOREIGN KEY (group_id) REFERENCES groups (id),
    CONSTRAINT fk_balances_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_balances_group_user UNIQUE (group_id, user_id)
);

CREATE TABLE settlements (
    id         UUID          PRIMARY KEY,
    group_id   UUID          NOT NULL,
    from_user  UUID          NOT NULL,
    to_user    UUID          NOT NULL,
    amount     NUMERIC(19,2) NOT NULL,
    settled_at TIMESTAMPTZ   NOT NULL,
    CONSTRAINT fk_settlements_group FOREIGN KEY (group_id) REFERENCES groups (id),
    CONSTRAINT fk_settlements_from_user FOREIGN KEY (from_user) REFERENCES users (id),
    CONSTRAINT fk_settlements_to_user FOREIGN KEY (to_user) REFERENCES users (id),
    CONSTRAINT ck_settlements_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_settlements_from_user_ne_to_user CHECK (from_user <> to_user)
);

CREATE INDEX idx_settlements_group_id ON settlements (group_id);
