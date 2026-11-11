CREATE TABLE idempotency_record (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 member_id bigint NOT NULL REFERENCES external_subject_binding(member_id), route text NOT NULL,
 idem_key text NOT NULL CHECK (length(idem_key) BETWEEN 1 AND 128), payload_hash text NOT NULL,
 response_status integer CHECK (response_status BETWEEN 100 AND 599), response_body jsonb,
 expires_at timestamptz NOT NULL, created_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE (member_id,route,idem_key),
 CHECK ((response_status IS NULL) = (response_body IS NULL))
);
CREATE INDEX idem_expiry_idx ON idempotency_record(expires_at);
CREATE TABLE notification_outbox (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 member_id bigint NOT NULL REFERENCES external_subject_binding(member_id), event_key text NOT NULL UNIQUE,
 channel text NOT NULL CHECK (channel IN ('IN_APP')),
 payload jsonb NOT NULL, attempts integer NOT NULL DEFAULT 0 CHECK (attempts >= 0),
 next_run_at timestamptz NOT NULL, delivered_at timestamptz, read_at timestamptz
);
CREATE INDEX outbox_pending_idx ON notification_outbox(next_run_at,id) WHERE delivered_at IS NULL;
CREATE TABLE metric_window (
 window_start timestamptz NOT NULL, metric text NOT NULL, value numeric NOT NULL,
 sample_count integer NOT NULL CHECK (sample_count >= 0), PRIMARY KEY (window_start,metric)
);
