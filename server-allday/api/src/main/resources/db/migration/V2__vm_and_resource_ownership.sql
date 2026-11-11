-- Extra allocation table: active VMIDs are globally unique across templates and rental VMs.
CREATE TABLE vmid_allocation (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 vmid integer NOT NULL CHECK (vmid >= 100), server_id bigint NOT NULL REFERENCES server_node(id),
 rental_id bigint, kind text NOT NULL CHECK (kind IN ('TEMPLATE','RENTAL')),
 allocated_at timestamptz NOT NULL DEFAULT now(), released_at timestamptz,
 FOREIGN KEY (rental_id,server_id) REFERENCES rental(id,server_id),
 CHECK ((kind = 'RENTAL') = (rental_id IS NOT NULL)),
 CHECK (released_at IS NULL OR released_at >= allocated_at),
 UNIQUE (id,server_id,vmid), UNIQUE (id,rental_id,server_id,vmid)
);
CREATE UNIQUE INDEX vmid_cluster_active_unique ON vmid_allocation(vmid) WHERE released_at IS NULL;
CREATE UNIQUE INDEX vmid_rental_active_unique ON vmid_allocation(rental_id) WHERE released_at IS NULL AND rental_id IS NOT NULL;
CREATE TABLE template_version (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 server_id bigint NOT NULL REFERENCES server_node(id), template_vmid integer NOT NULL,
 vmid_allocation_id bigint NOT NULL, image_hash text NOT NULL, driver_version text NOT NULL,
 approved_at timestamptz, deprecated_at timestamptz,
 FOREIGN KEY (vmid_allocation_id,server_id,template_vmid) REFERENCES vmid_allocation(id,server_id,vmid),
 UNIQUE (server_id,id)
);
ALTER TABLE server_node ADD CONSTRAINT active_template_same_server
 FOREIGN KEY (id,active_template_version_id) REFERENCES template_version(server_id,id);
CREATE TABLE vm_instance (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 rental_id bigint NOT NULL UNIQUE, server_id bigint NOT NULL,
 vmid integer NOT NULL, vmid_allocation_id bigint NOT NULL UNIQUE, ip inet,
 state text NOT NULL DEFAULT 'UNKNOWN' CHECK (state IN ('UNKNOWN','PROVISIONING','STOPPED','RUNNING','DELETING','DELETED','FAILED')),
 observed_at timestamptz, host_key_fingerprint text, access_enabled boolean NOT NULL DEFAULT false,
 FOREIGN KEY (rental_id,server_id) REFERENCES rental(id,server_id),
 FOREIGN KEY (vmid_allocation_id,rental_id,server_id,vmid) REFERENCES vmid_allocation(id,rental_id,server_id,vmid)
);
CREATE TABLE vm_credential (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 rental_id bigint NOT NULL REFERENCES rental(id), username text NOT NULL,
 credential_version integer NOT NULL CHECK (credential_version > 0), secret_ref text NOT NULL,
 encrypted_secret text NOT NULL, status text NOT NULL CHECK (status IN ('PENDING','ACTIVE','RETIRED','FAILED')),
 created_at timestamptz NOT NULL DEFAULT now(), UNIQUE (rental_id,credential_version)
);
CREATE UNIQUE INDEX credential_active_unique ON vm_credential(rental_id) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX credential_pending_unique ON vm_credential(rental_id) WHERE status = 'PENDING';
CREATE TABLE resource_manifest (
 rental_id bigint PRIMARY KEY, server_id bigint NOT NULL, vmid integer NOT NULL,
 volume_ids jsonb NOT NULL DEFAULT '[]', snippet_ids jsonb NOT NULL DEFAULT '[]',
 created_by_command_ids jsonb NOT NULL DEFAULT '[]', ingress_rule_ids jsonb NOT NULL DEFAULT '[]',
 FOREIGN KEY (rental_id,server_id) REFERENCES rental(id,server_id),
 CHECK (jsonb_typeof(volume_ids) = 'array'), CHECK (jsonb_typeof(snippet_ids) = 'array'),
 CHECK (jsonb_typeof(created_by_command_ids) = 'array'), CHECK (jsonb_typeof(ingress_rule_ids) = 'array')
);
CREATE TABLE ip_lease (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY, server_id bigint NOT NULL,
 rental_id bigint NOT NULL, address inet NOT NULL,
 allocated_at timestamptz NOT NULL DEFAULT now(), released_at timestamptz,
 FOREIGN KEY (rental_id,server_id) REFERENCES rental(id,server_id),
 CHECK (released_at IS NULL OR released_at >= allocated_at)
);
CREATE UNIQUE INDEX ip_active_per_node_unique ON ip_lease(server_id,address) WHERE released_at IS NULL;
CREATE UNIQUE INDEX ip_active_per_rental_unique ON ip_lease(rental_id) WHERE released_at IS NULL;
CREATE TABLE gpu_lease (
 server_id bigint PRIMARY KEY REFERENCES server_node(id), owner_rental_id bigint, owner_vmid integer,
 generation bigint NOT NULL DEFAULT 0 CHECK (generation >= 0),
 state text NOT NULL DEFAULT 'FREE' CHECK (state IN ('FREE','HELD','UNKNOWN')),
 changed_at timestamptz NOT NULL DEFAULT now(),
 FOREIGN KEY (owner_rental_id,server_id) REFERENCES rental(id,server_id),
 CHECK ((owner_rental_id IS NULL) = (owner_vmid IS NULL)),
 CHECK (state <> 'FREE' OR owner_rental_id IS NULL)
);
