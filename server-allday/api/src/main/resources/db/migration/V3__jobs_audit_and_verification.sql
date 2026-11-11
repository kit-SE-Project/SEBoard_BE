CREATE TABLE job (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 server_id bigint NOT NULL, rental_id bigint NOT NULL, slot_id bigint,
 intent_key text NOT NULL UNIQUE, type text NOT NULL CHECK (type IN ('PREPARE','RETURN','RESET_CREDENTIAL','START','STOP','SWITCH')),
 step text NOT NULL DEFAULT 'QUEUED', status text NOT NULL DEFAULT 'PENDING'
 CHECK (status IN ('PENDING','RUNNING','SUCCEEDED','FAILED','CANCELLED','SUPERSEDED')),
 desired_revision bigint NOT NULL CHECK (desired_revision >= 0), priority integer NOT NULL DEFAULT 0,
 worker_id text, locked_until timestamptz, operation_id text, upid text,
 retry_count integer NOT NULL DEFAULT 0 CHECK (retry_count >= 0), next_run_at timestamptz NOT NULL,
 reason_code text, created_at timestamptz NOT NULL DEFAULT now(),
 FOREIGN KEY (rental_id,server_id) REFERENCES rental(id,server_id),
 FOREIGN KEY (slot_id,rental_id,server_id) REFERENCES rental_slot(id,rental_id,server_id),
 CHECK ((status = 'RUNNING') = (worker_id IS NOT NULL AND locked_until IS NOT NULL))
);
CREATE UNIQUE INDEX job_one_running_per_server ON job(server_id) WHERE status = 'RUNNING';
CREATE INDEX job_due_idx ON job(next_run_at,priority DESC,id) WHERE status = 'PENDING';
CREATE INDEX job_rental_idx ON job(rental_id,id);
CREATE TABLE rental_event (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 rental_id bigint NOT NULL REFERENCES rental(id), job_id bigint REFERENCES job(id),
 operation_id text, upid text, actor_id bigint REFERENCES external_subject_binding(member_id),
 source text NOT NULL, request_id text NOT NULL, before_status text, after_status text,
 step text, reason_code text, reason_text text, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX rental_event_timeline_idx ON rental_event(rental_id,created_at,id);
CREATE TABLE return_verification (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, rental_id bigint NOT NULL REFERENCES rental(id),
 job_id bigint NOT NULL REFERENCES job(id), result text NOT NULL CHECK (result IN ('PASS','FAIL','UNKNOWN')),
 verified_at timestamptz NOT NULL, UNIQUE (job_id,verified_at)
);
CREATE TABLE verification_item (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 verification_id bigint NOT NULL REFERENCES return_verification(id),
 kind text NOT NULL CHECK (kind IN ('ACCESS_BLOCKED','VM_ABSENT','VOLUMES_ABSENT','SNIPPETS_ABSENT','INGRESS_ABSENT')),
 passed boolean, evidence jsonb NOT NULL, checked_at timestamptz NOT NULL,
 UNIQUE (verification_id,kind)
);
