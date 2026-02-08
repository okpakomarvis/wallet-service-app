CREATE TABLE kyc_verifications
(
    id                             UUID        NOT NULL,
    user_id                        UUID        NOT NULL,
    level                          VARCHAR(20) NOT NULL,
    status                         VARCHAR(20) NOT NULL,
    full_name                      VARCHAR(100),
    id_type                        VARCHAR(50),
    id_number                      VARCHAR(50),
    date_of_birth                  date,
    nationality                    VARCHAR(100),
    address                        VARCHAR(500),
    city                           VARCHAR(100),
    state                          VARCHAR(100),
    postal_code                    VARCHAR(20),
    country                        VARCHAR(50),
    id_document_url                VARCHAR(500),
    proof_of_address_url           VARCHAR(500),
    selfie_url                     VARCHAR(500),
    id_document_public_id          VARCHAR(300),
    proof_of_address_public_id     VARCHAR(300),
    selfie_public_id               VARCHAR(300),
    id_document_resource_type      VARCHAR(30),
    proof_of_address_resource_type VARCHAR(30),
    selfie_resource_type           VARCHAR(30),
    verification_provider          VARCHAR(100),
    external_verification_id       VARCHAR(100),
    verified_at                    TIMESTAMP WITHOUT TIME ZONE,
    rejection_reason               VARCHAR(1000),
    reviewed_by                    UUID,
    reviewed_at                    TIMESTAMP WITHOUT TIME ZONE,
    created_at                     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at                     TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_kyc_verifications PRIMARY KEY (id)
);

CREATE TABLE ledger_entries
(
    id                    UUID           NOT NULL,
    wallet_id             UUID           NOT NULL,
    entry_type            VARCHAR(255)   NOT NULL,
    amount                DECIMAL(19, 4) NOT NULL,
    balance_before        DECIMAL(19, 4) NOT NULL,
    balance_after         DECIMAL(19, 4) NOT NULL,
    transaction_reference VARCHAR(100)   NOT NULL,
    idempotency_key       VARCHAR(100)   NOT NULL,
    description           VARCHAR(500),
    external_reference    VARCHAR(50),
    created_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    ip_address            VARCHAR(45),
    CONSTRAINT pk_ledger_entries PRIMARY KEY (id)
);

CREATE TABLE notifications
(
    id            UUID          NOT NULL,
    user_id       UUID          NOT NULL,
    type          VARCHAR(255)  NOT NULL,
    title         VARCHAR(200)  NOT NULL,
    message       VARCHAR(1000) NOT NULL,
    reference_id  VARCHAR(100),
    channel       VARCHAR(255)  NOT NULL,
    priority      VARCHAR(255)  NOT NULL,
    is_read       BOOLEAN       NOT NULL,
    read_at       TIMESTAMP WITHOUT TIME ZONE,
    is_sent       BOOLEAN       NOT NULL,
    sent_at       TIMESTAMP WITHOUT TIME ZONE,
    metadata      VARCHAR(1000),
    retry_count   INTEGER,
    error_message VARCHAR(500),
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id)
);

CREATE TABLE transactions
(
    id                    UUID           NOT NULL,
    reference             VARCHAR(50)    NOT NULL,
    user_id               UUID,
    source_wallet_id      UUID,
    destination_wallet_id UUID,
    type                  VARCHAR(255)   NOT NULL,
    amount                DECIMAL(19, 4) NOT NULL,
    fee                   DECIMAL(19, 4),
    currency              VARCHAR(3)     NOT NULL,
    status                VARCHAR(255)   NOT NULL,
    description           VARCHAR(500),
    failure_reason        VARCHAR(1000),
    external_reference    VARCHAR(100),
    payment_gateway       VARCHAR(50),
    ip_address            VARCHAR(45),
    user_agent            VARCHAR(500),
    created_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITHOUT TIME ZONE,
    completed_at          TIMESTAMP WITHOUT TIME ZONE,
    version               BIGINT,
    CONSTRAINT pk_transactions PRIMARY KEY (id)
);

CREATE TABLE user_roles
(
    user_id UUID         NOT NULL,
    role    VARCHAR(255) NOT NULL
);

CREATE TABLE users
(
    id                          UUID         NOT NULL,
    email                       VARCHAR(100) NOT NULL,
    password_hash               VARCHAR(255) NOT NULL,
    first_name                  VARCHAR(100) NOT NULL,
    last_name                   VARCHAR(100) NOT NULL,
    phone_number                VARCHAR(20),
    status                      VARCHAR(20)  NOT NULL,
    kyc_status                  VARCHAR(20)  NOT NULL,
    kyc_level                   VARCHAR(20)  NOT NULL,
    profile_image_url           VARCHAR(500),
    profile_image_public_id     VARCHAR(255),
    profile_image_resource_type VARCHAR(50),
    mfa_enabled                 BOOLEAN      NOT NULL,
    mfa_secret                  VARCHAR(32),
    transaction_pin             VARCHAR(100),
    created_at                  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at                  TIMESTAMP WITHOUT TIME ZONE,
    last_login_at               TIMESTAMP WITHOUT TIME ZONE,
    last_login_ip               VARCHAR(45),
    version                     BIGINT,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE TABLE wallets
(
    id                UUID           NOT NULL,
    user_id           UUID           NOT NULL,
    wallet_number     VARCHAR(20)    NOT NULL,
    currency          VARCHAR(3)     NOT NULL,
    balance           DECIMAL(19, 4) NOT NULL,
    available_balance DECIMAL(19, 4) NOT NULL,
    status            VARCHAR(255)   NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITHOUT TIME ZONE,
    version           BIGINT,
    CONSTRAINT pk_wallets PRIMARY KEY (id)
);

ALTER TABLE ledger_entries
    ADD CONSTRAINT uc_ledger_entries_idempotencykey UNIQUE (idempotency_key);

ALTER TABLE transactions
    ADD CONSTRAINT uc_transactions_reference UNIQUE (reference);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_users_phonenumber UNIQUE (phone_number);

ALTER TABLE wallets
    ADD CONSTRAINT uc_wallets_walletnumber UNIQUE (wallet_number);

ALTER TABLE kyc_verifications
    ADD CONSTRAINT uk_user_kyc_level UNIQUE (user_id, level);

CREATE INDEX idx_destination_wallet ON transactions (destination_wallet_id, created_at);

CREATE INDEX idx_email ON users (email);

CREATE UNIQUE INDEX idx_idempotency ON ledger_entries (idempotency_key);

CREATE INDEX idx_kyc_level ON users (kyc_level);

CREATE INDEX idx_kyc_status ON kyc_verifications (status);

CREATE INDEX idx_notification_read ON notifications (is_read);

CREATE INDEX idx_notification_type ON notifications (type, created_at);

CREATE INDEX idx_notification_user ON notifications (user_id, created_at);

CREATE INDEX idx_phone ON users (phone_number);

CREATE UNIQUE INDEX idx_reference ON transactions (reference);

CREATE INDEX idx_source_wallet ON transactions (source_wallet_id, created_at);

CREATE INDEX idx_status ON transactions (status, created_at);

CREATE INDEX idx_transaction_ref ON ledger_entries (transaction_reference);

CREATE UNIQUE INDEX idx_user_currency ON wallets (user_id, currency);

CREATE INDEX idx_wallet_created ON ledger_entries (wallet_id, created_at);

CREATE UNIQUE INDEX idx_wallet_number ON wallets (wallet_number);

ALTER TABLE kyc_verifications
    ADD CONSTRAINT FK_KYC_VERIFICATIONS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

CREATE INDEX idx_kyc_user ON kyc_verifications (user_id);

ALTER TABLE ledger_entries
    ADD CONSTRAINT FK_LEDGER_ENTRIES_ON_WALLET FOREIGN KEY (wallet_id) REFERENCES wallets (id);

ALTER TABLE notifications
    ADD CONSTRAINT FK_NOTIFICATIONS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE transactions
    ADD CONSTRAINT FK_TRANSACTIONS_ON_DESTINATION_WALLET FOREIGN KEY (destination_wallet_id) REFERENCES wallets (id);

ALTER TABLE transactions
    ADD CONSTRAINT FK_TRANSACTIONS_ON_SOURCE_WALLET FOREIGN KEY (source_wallet_id) REFERENCES wallets (id);

ALTER TABLE transactions
    ADD CONSTRAINT FK_TRANSACTIONS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE wallets
    ADD CONSTRAINT FK_WALLETS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE user_roles
    ADD CONSTRAINT fk_user_roles_on_user FOREIGN KEY (user_id) REFERENCES users (id);