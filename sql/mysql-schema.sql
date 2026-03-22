CREATE DATABASE IF NOT EXISTS claw_pond
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE claw_pond;

CREATE TABLE IF NOT EXISTS user_accounts (
    id CHAR(36) NOT NULL,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled TINYINT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_accounts_username (username),
    UNIQUE KEY uk_user_accounts_email (email),
    KEY idx_user_accounts_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS tags (
    id CHAR(36) NOT NULL,
    name VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tags_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS openclaw_instances (
    id CHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    base_url VARCHAR(255) NOT NULL,
    external_id VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    active TINYINT(1) NOT NULL,
    api_token VARCHAR(255) NULL,
    owner_id CHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_openclaw_instances_base_url (base_url),
    UNIQUE KEY uk_openclaw_instances_external_id (external_id),
    KEY idx_openclaw_instances_owner_id (owner_id),
    KEY idx_openclaw_instances_active_created_at (active, created_at),
    CONSTRAINT fk_openclaw_instances_owner
        FOREIGN KEY (owner_id) REFERENCES user_accounts (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS openclaw_instance_tags (
    openclaw_instance_id CHAR(36) NOT NULL,
    tag_id CHAR(36) NOT NULL,
    PRIMARY KEY (openclaw_instance_id, tag_id),
    KEY idx_openclaw_instance_tags_tag_id (tag_id),
    CONSTRAINT fk_openclaw_instance_tags_instance
        FOREIGN KEY (openclaw_instance_id) REFERENCES openclaw_instances (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,
    CONSTRAINT fk_openclaw_instance_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS lobster_assets (
    id CHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    media_type VARCHAR(120) NULL,
    file_size BIGINT NOT NULL,
    owner_id CHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_lobster_assets_stored_filename (stored_filename),
    KEY idx_lobster_assets_owner_id (owner_id),
    KEY idx_lobster_assets_created_at (created_at),
    CONSTRAINT fk_lobster_assets_owner
        FOREIGN KEY (owner_id) REFERENCES user_accounts (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS lobster_asset_tags (
    lobster_asset_id CHAR(36) NOT NULL,
    tag_id CHAR(36) NOT NULL,
    PRIMARY KEY (lobster_asset_id, tag_id),
    KEY idx_lobster_asset_tags_tag_id (tag_id),
    CONSTRAINT fk_lobster_asset_tags_asset
        FOREIGN KEY (lobster_asset_id) REFERENCES lobster_assets (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,
    CONSTRAINT fk_lobster_asset_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS work_jobs (
    id CHAR(36) NOT NULL,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL,
    requester_id CHAR(36) NOT NULL,
    openclaw_instance_id CHAR(36) NOT NULL,
    lobster_asset_id CHAR(36) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_work_jobs_requester_id (requester_id),
    KEY idx_work_jobs_openclaw_instance_id (openclaw_instance_id),
    KEY idx_work_jobs_lobster_asset_id (lobster_asset_id),
    KEY idx_work_jobs_status_created_at (status, created_at),
    CONSTRAINT fk_work_jobs_requester
        FOREIGN KEY (requester_id) REFERENCES user_accounts (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    CONSTRAINT fk_work_jobs_openclaw_instance
        FOREIGN KEY (openclaw_instance_id) REFERENCES openclaw_instances (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    CONSTRAINT fk_work_jobs_lobster_asset
        FOREIGN KEY (lobster_asset_id) REFERENCES lobster_assets (id)
        ON DELETE SET NULL
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS work_job_tags (
    work_job_id CHAR(36) NOT NULL,
    tag_id CHAR(36) NOT NULL,
    PRIMARY KEY (work_job_id, tag_id),
    KEY idx_work_job_tags_tag_id (tag_id),
    CONSTRAINT fk_work_job_tags_job
        FOREIGN KEY (work_job_id) REFERENCES work_jobs (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,
    CONSTRAINT fk_work_job_tags_tag
        FOREIGN KEY (tag_id) REFERENCES tags (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
