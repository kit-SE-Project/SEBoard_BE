ALTER TABLE server_node ADD COLUMN deleted_at timestamptz;
ALTER TABLE server_node ADD COLUMN deleted_by bigint;
ALTER TABLE server_node ADD COLUMN deleted_reason text;
ALTER TABLE server_node ADD CONSTRAINT server_deletion_metadata CHECK (
 (deleted_at IS NULL AND deleted_by IS NULL AND deleted_reason IS NULL) OR
 (deleted_at IS NOT NULL AND deleted_by IS NOT NULL AND length(trim(deleted_reason)) BETWEEN 1 AND 500)
);

ALTER TABLE rental ADD COLUMN server_snapshot jsonb;
-- Older rentals cannot reconstruct information edited before this migration.
UPDATE rental r SET server_snapshot=jsonb_build_object(
 'id',s.id,'categoryId',s.category_id,'categoryName',c.name,'displayName',s.display_name,
 'nodeName',s.node_name,'cpuModel',s.cpu_model,'memoryGb',s.memory_gb,'gpuModel',s.gpu_model,
 'capturedAt',now(),'origin','BACKFILL') FROM server_node s JOIN category c ON c.id=s.category_id WHERE s.id=r.server_id;
ALTER TABLE rental ALTER COLUMN server_snapshot SET NOT NULL;
ALTER TABLE rental ADD CONSTRAINT rental_snapshot_object CHECK (jsonb_typeof(server_snapshot)='object');

CREATE FUNCTION capture_rental_server_snapshot() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE s record;
BEGIN
 IF TG_OP='UPDATE' THEN
  IF NEW.server_snapshot IS DISTINCT FROM OLD.server_snapshot OR NEW.server_id<>OLD.server_id THEN
   RAISE EXCEPTION 'rental server snapshot is immutable' USING ERRCODE='23514';
  END IF;
 ELSE
  SELECT n.*,c.name AS category_name INTO s FROM server_node n JOIN category c ON c.id=n.category_id WHERE n.id=NEW.server_id FOR UPDATE OF n;
  IF NOT FOUND OR s.deleted_at IS NOT NULL THEN RAISE EXCEPTION 'deactivated server cannot accept rental' USING ERRCODE='23514'; END IF;
  NEW.server_snapshot=jsonb_build_object('id',s.id,'categoryId',s.category_id,'categoryName',s.category_name,
    'displayName',s.display_name,'nodeName',s.node_name,'cpuModel',s.cpu_model,'memoryGb',s.memory_gb,
    'gpuModel',s.gpu_model,'capturedAt',now(),'origin','RESERVATION');
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER rental_snapshot_guard BEFORE INSERT OR UPDATE OF server_snapshot,server_id ON rental FOR EACH ROW EXECUTE FUNCTION capture_rental_server_snapshot();

CREATE FUNCTION guard_server_deactivation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL THEN
  IF EXISTS(SELECT 1 FROM rental WHERE server_id=NEW.id AND status NOT IN ('RETURNED','CANCELLED'))
    OR EXISTS(SELECT 1 FROM job WHERE server_id=NEW.id AND status='RUNNING')
    OR EXISTS(SELECT 1 FROM vmid_allocation WHERE server_id=NEW.id AND kind='RENTAL' AND released_at IS NULL)
    OR EXISTS(SELECT 1 FROM ip_lease WHERE server_id=NEW.id AND released_at IS NULL)
    OR EXISTS(SELECT 1 FROM gpu_lease WHERE server_id=NEW.id AND (owner_rental_id IS NOT NULL OR state<>'FREE'))
    OR EXISTS(SELECT 1 FROM vm_instance WHERE server_id=NEW.id AND state<>'DELETED') THEN
   RAISE EXCEPTION 'server still owns rental or uncertain resources' USING ERRCODE='23514';
  END IF;
 END IF;
 IF OLD.deleted_at IS NOT NULL AND (NEW.deleted_at IS DISTINCT FROM OLD.deleted_at OR NEW.deleted_by IS DISTINCT FROM OLD.deleted_by OR NEW.deleted_reason IS DISTINCT FROM OLD.deleted_reason) THEN
  RAISE EXCEPTION 'server deactivation is permanent' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER server_deactivation_guard BEFORE UPDATE OF deleted_at,deleted_by,deleted_reason ON server_node FOR EACH ROW EXECUTE FUNCTION guard_server_deactivation();

CREATE TABLE server_metadata_event (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 server_id bigint NOT NULL REFERENCES server_node(id),
 actor_id bigint NOT NULL REFERENCES external_subject_binding(member_id),
 request_id text NOT NULL,
 action text NOT NULL CHECK(action IN ('METADATA_UPDATED','DEACTIVATED')),
 reason text NOT NULL CHECK(length(trim(reason)) BETWEEN 1 AND 500),
 before_data jsonb NOT NULL,after_data jsonb NOT NULL,created_at timestamptz NOT NULL
);
CREATE INDEX server_metadata_event_server_idx ON server_metadata_event(server_id,id DESC);
