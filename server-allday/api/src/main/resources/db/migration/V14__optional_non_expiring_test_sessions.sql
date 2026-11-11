ALTER TABLE auth_session ADD COLUMN no_expiry boolean NOT NULL DEFAULT false;
ALTER TABLE auth_session ALTER COLUMN expires_at DROP NOT NULL;
ALTER TABLE auth_session ADD CONSTRAINT auth_session_expiry_matches_mode
 CHECK ((no_expiry AND expires_at IS NULL) OR (NOT no_expiry AND expires_at IS NOT NULL));
