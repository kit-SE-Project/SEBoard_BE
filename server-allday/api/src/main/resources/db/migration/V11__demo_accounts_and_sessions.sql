-- Demo identity mirrors SEBoard's account -> roles -> board user model.
-- The reservation member ID remains a separate, stable internal key.
CREATE SEQUENCE external_member_id_seq START WITH 10000;
ALTER SEQUENCE external_member_id_seq OWNED BY external_subject_binding.member_id;
ALTER TABLE external_subject_binding ALTER COLUMN member_id SET DEFAULT nextval('external_member_id_seq');
CREATE TABLE accounts (
 account_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 login_id text NOT NULL UNIQUE CHECK (length(login_id) BETWEEN 1 AND 254),
 name text NOT NULL, password_hash text NOT NULL,
 status text NOT NULL DEFAULT 'NORMAL' CHECK(status IN ('NORMAL','SUSPENDED','TEMP_DELETED','PERMANENT_DELETED')),
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE roles (
 role_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 name text NOT NULL UNIQUE CHECK (name ~ '^ROLE_[A-Z_]+$'), description text NOT NULL
);
CREATE TABLE role_accounts (
 account_id bigint NOT NULL REFERENCES accounts(account_id) ON DELETE CASCADE,
 role_id bigint NOT NULL REFERENCES roles(role_id), PRIMARY KEY(account_id,role_id)
);
CREATE TABLE board_users (
 board_user_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 account_id bigint NOT NULL UNIQUE REFERENCES accounts(account_id),
 member_id bigint NOT NULL UNIQUE REFERENCES external_subject_binding(member_id),
 nickname text NOT NULL
);
CREATE TABLE auth_session (
 session_id uuid PRIMARY KEY, account_id bigint NOT NULL REFERENCES accounts(account_id),
 refresh_hash text NOT NULL UNIQUE, refresh_kind text NOT NULL CHECK(refresh_kind IN ('refreshToken','largeRefreshToken')),
 expires_at timestamptz NOT NULL, revoked_at timestamptz, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX auth_session_account_idx ON auth_session(account_id);
CREATE TABLE auth_login_throttle (
 login_id text PRIMARY KEY, failures integer NOT NULL DEFAULT 0 CHECK(failures >= 0),
 window_start timestamptz NOT NULL, blocked_until timestamptz
);
