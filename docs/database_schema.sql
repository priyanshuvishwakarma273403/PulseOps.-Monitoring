-- MySQL 8.0 normalized database schema for PulseOps Observability Platform

CREATE DATABASE IF NOT EXISTS pulseops;
USE pulseops;

-- 1. Roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- 2. Permissions table
CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- 3. Role Permissions mapping
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- 4. Organizations (Tenants)
CREATE TABLE IF NOT EXISTS organizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    INDEX idx_org_slug (slug)
);

-- 5. Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    is_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_email (email)
);

-- 6. Organization Members mapping
CREATE TABLE IF NOT EXISTS organization_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    invited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    joined_at TIMESTAMP NULL,
    status VARCHAR(50) NOT NULL, -- INVITED, ACTIVE, SUSPENDED
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id),
    UNIQUE KEY uq_org_user (organization_id, user_id),
    INDEX idx_member_user (user_id)
);

-- 7. Refresh Tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    is_revoked BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_token_string (token)
);

-- 8. API Keys
CREATE TABLE IF NOT EXISTS api_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    prefix VARCHAR(10) NOT NULL,
    created_by BIGINT NOT NULL,
    expires_at TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    INDEX idx_key_hash (key_hash)
);

-- 9. Monitors configuration
CREATE TABLE IF NOT EXISTS monitors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL, -- REST, GRAPHQL, WEBSOCKET, SSL, DNS
    url VARCHAR(512) NOT NULL,
    method VARCHAR(10) NOT NULL,
    headers TEXT, -- JSON structure
    request_body TEXT,
    expected_status_code INT DEFAULT 200,
    expected_response_time INT DEFAULT 1000, -- milliseconds
    check_interval INT DEFAULT 60, -- seconds
    status VARCHAR(50) NOT NULL, -- ACTIVE, PAUSED
    health_status VARCHAR(50) DEFAULT 'UP', -- UP, DOWN, DEGRADED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    INDEX idx_monitors_org_deleted (organization_id, is_deleted),
    INDEX idx_monitors_active (status, is_deleted)
);

-- 10. Monitor Checks execution logs
CREATE TABLE IF NOT EXISTS monitor_checks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    monitor_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL, -- UP, DOWN, DEGRADED
    response_time INT, -- milliseconds
    status_code INT,
    error_message TEXT,
    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (monitor_id) REFERENCES monitors(id) ON DELETE CASCADE,
    INDEX idx_checks_monitor_time (monitor_id, checked_at)
);

-- 11. Incidents
CREATE TABLE IF NOT EXISTS incidents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    monitor_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    severity VARCHAR(50) NOT NULL, -- CRITICAL, HIGH, MEDIUM, LOW
    status VARCHAR(50) NOT NULL, -- OPEN, INVESTIGATING, RESOLVED, CLOSED
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    assigned_user_id BIGINT NULL,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (monitor_id) REFERENCES monitors(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_user_id) REFERENCES users(id) ON SET NULL,
    INDEX idx_incidents_org_status (organization_id, status)
);

-- 12. Incident Comments for timeline collaboration
CREATE TABLE IF NOT EXISTS incident_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    comment TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (incident_id) REFERENCES incidents(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_comments_incident (incident_id)
);

-- 13. Alert Rules
CREATE TABLE IF NOT EXISTS alert_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    monitor_id BIGINT NOT NULL,
    metric_type VARCHAR(50) NOT NULL, -- LATENCY, ERROR_RATE, AVAILABILITY, STATUS_DOWN
    operator VARCHAR(10) NOT NULL, -- >, <, =
    threshold DOUBLE NOT NULL,
    duration_seconds INT DEFAULT 60,
    channels TEXT, -- JSON Array: ["email", "slack"]
    is_enabled BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (monitor_id) REFERENCES monitors(id) ON DELETE CASCADE,
    INDEX idx_rules_monitor (monitor_id, is_enabled)
);

-- 14. Alert History log
CREATE TABLE IF NOT EXISTS alert_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    alert_rule_id BIGINT NOT NULL,
    monitor_id BIGINT NOT NULL,
    incident_id BIGINT NULL,
    message TEXT NOT NULL,
    channel VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL, -- SENT, FAILED
    fired_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    FOREIGN KEY (alert_rule_id) REFERENCES alert_rules(id) ON DELETE CASCADE,
    FOREIGN KEY (monitor_id) REFERENCES monitors(id) ON DELETE CASCADE,
    FOREIGN KEY (incident_id) REFERENCES incidents(id) ON SET NULL,
    INDEX idx_history_org (organization_id)
);

-- 15. Subscriptions and billing plan details
CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    plan_name VARCHAR(50) NOT NULL, -- FREE, TEAM, ENTERPRISE
    status VARCHAR(50) NOT NULL, -- ACTIVE, PAST_DUE, CANCELED
    billing_period_start TIMESTAMP NOT NULL,
    billing_period_end TIMESTAMP NOT NULL,
    stripe_subscription_id VARCHAR(255) NULL,
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

-- 16. Audit Logging
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NULL,
    user_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id BIGINT NULL,
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_org (organization_id)
);

-- Initial seed data
INSERT INTO roles (id, name) VALUES (1, 'SUPER_ADMIN') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO roles (id, name) VALUES (2, 'ORG_ADMIN') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO roles (id, name) VALUES (3, 'DEVELOPER') ON DUPLICATE KEY UPDATE name=name;
INSERT INTO roles (id, name) VALUES (4, 'VIEWER') ON DUPLICATE KEY UPDATE name=name;
