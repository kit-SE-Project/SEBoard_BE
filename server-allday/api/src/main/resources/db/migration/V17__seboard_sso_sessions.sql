CREATE TABLE allday_sso_login (
    state_hash text PRIMARY KEY,
    browser_hash text NOT NULL,
    verifier text NOT NULL,
    expires_at timestamptz NOT NULL
);
CREATE INDEX allday_sso_login_expiry_idx ON allday_sso_login(expires_at);
CREATE INDEX allday_sso_login_browser_idx ON allday_sso_login(browser_hash);

CREATE TABLE allday_sso_session (
    token_hash text PRIMARY KEY,
    member_id bigint NOT NULL REFERENCES external_subject_binding(member_id),
    board_grant text NOT NULL,
    expires_at timestamptz NOT NULL
);
CREATE INDEX allday_sso_session_expiry_idx ON allday_sso_session(expires_at);
