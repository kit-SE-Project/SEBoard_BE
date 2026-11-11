CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE category (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 name text NOT NULL UNIQUE CHECK (length(trim(name)) BETWEEN 1 AND 100),
 description text, version bigint NOT NULL DEFAULT 0 CHECK (version >= 0)
);
CREATE TABLE server_node (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 category_id bigint NOT NULL REFERENCES category(id),
 display_name text NOT NULL, node_name text NOT NULL UNIQUE,
 cpu_model text NOT NULL, memory_gb integer NOT NULL CHECK (memory_gb > 0),
 gpu_model text NOT NULL, agent_endpoint_ref text, secret_ref text,
 node_ingress_port integer CHECK (node_ingress_port BETWEEN 1 AND 65535),
 vm_subnet cidr, gpu_mapping_ref text, active_template_version_id bigint,
 status text NOT NULL DEFAULT 'UNKNOWN' CHECK (status IN ('AVAILABLE','MAINTENANCE','QUARANTINED','UNKNOWN')),
 observed_at timestamptz, inventory_source text NOT NULL DEFAULT 'MANUAL' CHECK (inventory_source IN ('MANUAL','AGENT','DEMO')),
 version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
 status_reason text, changed_by bigint, changed_at timestamptz
);
CREATE TABLE external_subject_binding (
 member_id bigint PRIMARY KEY, issuer text NOT NULL, subject text NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now(), last_checked_at timestamptz,
 last_eligibility boolean, UNIQUE (issuer, subject)
);
CREATE TABLE rental (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 member_id bigint NOT NULL REFERENCES external_subject_binding(member_id),
 server_id bigint NOT NULL REFERENCES server_node(id),
 -- CANCELLED is an explicit proposed extension for cancellation before any VM resource exists.
 status text NOT NULL DEFAULT 'RESERVED' CHECK (status IN
 ('RESERVED','PROVISIONING','READY','ACTIVE','RETAINED','RETURNING','RETURNED','CANCELLED')),
 retained_reason text, contract_start timestamptz NOT NULL, contract_end timestamptz NOT NULL,
 purpose text NOT NULL CHECK (length(trim(purpose)) BETWEEN 1 AND 2000),
 desired_revision bigint NOT NULL DEFAULT 0 CHECK (desired_revision >= 0), reason_code text,
 version bigint NOT NULL DEFAULT 0 CHECK (version >= 0), created_at timestamptz NOT NULL DEFAULT now(),
 CHECK (contract_start < contract_end),
 CHECK ((status = 'RETAINED') = (retained_reason IS NOT NULL)),
 CHECK (retained_reason IS NULL OR retained_reason = 'BETWEEN_SLOTS'),
 UNIQUE (id, server_id)
);
CREATE TABLE rental_slot (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 rental_id bigint NOT NULL, server_id bigint NOT NULL,
 start_at timestamptz NOT NULL, end_at timestamptz NOT NULL,
 blocked_start timestamptz NOT NULL, blocked_end timestamptz NOT NULL,
 state text NOT NULL DEFAULT 'SCHEDULED' CHECK (state IN ('SCHEDULED','ACTIVE','COMPLETED','CANCELLED')),
 FOREIGN KEY (rental_id,server_id) REFERENCES rental(id,server_id),
 CHECK (start_at < end_at), CHECK (blocked_start <= start_at AND end_at <= blocked_end),
 CONSTRAINT slot_no_overlap EXCLUDE USING gist
 (server_id WITH =, tstzrange(blocked_start,blocked_end,'[)') WITH &&)
 WHERE (state <> 'CANCELLED'),
 UNIQUE (id, rental_id, server_id)
);
CREATE INDEX server_category_idx ON server_node(category_id);
CREATE INDEX rental_member_created_idx ON rental(member_id,created_at DESC,id DESC);
CREATE INDEX rental_server_idx ON rental(server_id);
CREATE INDEX slot_rental_idx ON rental_slot(rental_id,server_id);
