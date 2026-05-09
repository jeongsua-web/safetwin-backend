-- SafeTwin 데이터베이스 스키마
-- PostgreSQL 14+

-- ── 확장 ────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ── users ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id           BIGSERIAL PRIMARY KEY,
    email        VARCHAR(100) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    name         VARCHAR(50)  NOT NULL,
    phone        VARCHAR(20),
    biz_number   VARCHAR(20),
    industry     VARCHAR(100),
    company_size VARCHAR(50),
    role         VARCHAR(20)  NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'MANAGER', 'WORKER')),
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ── refresh_tokens ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- ── sites ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sites (
    id         BIGSERIAL PRIMARY KEY,
    manager_id BIGINT       NOT NULL REFERENCES users(id),
    name       VARCHAR(100) NOT NULL,
    address    VARCHAR(200),
    biz_number VARCHAR(20),
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'COMPLETED')),
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sites_manager_id ON sites(manager_id);

-- ── zones ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS zones (
    id           BIGSERIAL PRIMARY KEY,
    site_id      BIGINT        NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    name         VARCHAR(100)  NOT NULL,
    description  VARCHAR(200),
    floor_number INTEGER,
    x            DOUBLE PRECISION,
    y            DOUBLE PRECISION,
    w            DOUBLE PRECISION,
    h            DOUBLE PRECISION,
    area         VARCHAR(50),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_zones_site_id ON zones(site_id);

-- ── analyses ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS analyses (
    id            BIGSERIAL PRIMARY KEY,
    zone_id       BIGINT       NOT NULL REFERENCES zones(id),
    requester_id  BIGINT       NOT NULL REFERENCES users(id),
    image_url     VARCHAR(500),
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),
    ai_result     TEXT,
    overall_score INTEGER,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_analyses_zone_id      ON analyses(zone_id);
CREATE INDEX IF NOT EXISTS idx_analyses_requester_id ON analyses(requester_id);
CREATE INDEX IF NOT EXISTS idx_analyses_status        ON analyses(status);
CREATE INDEX IF NOT EXISTS idx_analyses_created_at    ON analyses(created_at DESC);

-- ── risks ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS risks (
    id          BIGSERIAL PRIMARY KEY,
    analysis_id BIGINT       NOT NULL REFERENCES analyses(id) ON DELETE CASCADE,
    label       VARCHAR(100) NOT NULL,
    description VARCHAR(200) NOT NULL,
    law         TEXT,
    action      TEXT,
    level       VARCHAR(20)  NOT NULL CHECK (level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    x           DOUBLE PRECISION,
    y           DOUBLE PRECISION,
    location    VARCHAR(500),
    status      VARCHAR(20)  NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED')),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_risks_analysis_id ON risks(analysis_id);
CREATE INDEX IF NOT EXISTS idx_risks_status      ON risks(status);
CREATE INDEX IF NOT EXISTS idx_risks_level       ON risks(level);

-- ── documents ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS documents (
    id          BIGSERIAL PRIMARY KEY,
    site_id     BIGINT       NOT NULL REFERENCES sites(id),
    uploader_id BIGINT       NOT NULL REFERENCES users(id),
    analysis_id BIGINT       REFERENCES analyses(id),
    title       VARCHAR(200) NOT NULL,
    type        VARCHAR(30)  NOT NULL CHECK (type IN ('SAFETY_PLAN', 'INSPECTION_REPORT', 'RISK_ASSESSMENT', 'EDUCATION_CERT', 'GROUP_PHOTO', 'OTHER')),
    status      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SIGNED')),
    file_url    VARCHAR(500) NOT NULL,
    file_size   BIGINT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_documents_site_id     ON documents(site_id);
CREATE INDEX IF NOT EXISTS idx_documents_uploader_id ON documents(uploader_id);
CREATE INDEX IF NOT EXISTS idx_documents_type        ON documents(type);
CREATE INDEX IF NOT EXISTS idx_documents_status      ON documents(status);

-- ── signatures ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS signatures (
    id                  BIGSERIAL PRIMARY KEY,
    document_id         BIGINT       NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    signer_id           BIGINT       REFERENCES users(id),
    signer_name         VARCHAR(50)  NOT NULL,
    signature_data      TEXT,
    signed_at           TIMESTAMP,
    signature_image_url VARCHAR(500),
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_signatures_document_id ON signatures(document_id);

-- ── workers ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS workers (
    id         BIGSERIAL PRIMARY KEY,
    site_id    BIGINT       NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    user_id    BIGINT       REFERENCES users(id),
    name       VARCHAR(50)  NOT NULL,
    phone      VARCHAR(20),
    occupation VARCHAR(50),
    start_date DATE,
    end_date   DATE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_workers_site_id ON workers(site_id);
CREATE INDEX IF NOT EXISTS idx_workers_user_id ON workers(user_id);
