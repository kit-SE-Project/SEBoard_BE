ALTER TABLE job ADD COLUMN result_payload_hash text;
ALTER TABLE job ADD COLUMN result_response jsonb;
ALTER TABLE vmid_allocation ADD CONSTRAINT allocation_rental_vmid_unique UNIQUE (rental_id,server_id,vmid);
ALTER TABLE resource_manifest ADD CONSTRAINT manifest_owned_vmid
 FOREIGN KEY (rental_id,server_id,vmid) REFERENCES vmid_allocation(rental_id,server_id,vmid);

CREATE FUNCTION guard_returned() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.status='RETURNED' AND OLD.status<>'RETURNED' THEN
  IF OLD.status<>'RETURNING' OR NOT EXISTS (
   SELECT 1 FROM return_verification v JOIN job j ON j.id=v.job_id
   WHERE v.rental_id=NEW.id AND j.rental_id=NEW.id AND j.type='RETURN'
    AND j.desired_revision=NEW.desired_revision AND j.status='SUCCEEDED' AND v.result='PASS'
    AND (SELECT count(*) FROM verification_item i WHERE i.verification_id=v.id)=5
    AND NOT EXISTS (SELECT 1 FROM verification_item i WHERE i.verification_id=v.id AND i.passed IS DISTINCT FROM true)
  ) THEN RAISE EXCEPTION 'RETURNED requires complete current return verification' USING ERRCODE='23514'; END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER rental_returned_guard BEFORE UPDATE ON rental FOR EACH ROW EXECUTE FUNCTION guard_returned();

CREATE FUNCTION guard_lease_release() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF OLD.released_at IS NULL AND NEW.released_at IS NOT NULL AND NEW.rental_id IS NOT NULL THEN
  IF NOT EXISTS(SELECT 1 FROM rental WHERE id=NEW.rental_id AND status='RETURNED') THEN
   RAISE EXCEPTION 'resource lease cannot be released before verified return' USING ERRCODE='23514';
  END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER vmid_release_guard BEFORE UPDATE ON vmid_allocation FOR EACH ROW EXECUTE FUNCTION guard_lease_release();
CREATE TRIGGER ip_release_guard BEFORE UPDATE ON ip_lease FOR EACH ROW EXECUTE FUNCTION guard_lease_release();

CREATE FUNCTION immutable_allocation_identity() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF ROW(NEW.vmid,NEW.server_id,NEW.rental_id,NEW.kind) IS DISTINCT FROM ROW(OLD.vmid,OLD.server_id,OLD.rental_id,OLD.kind) THEN
  RAISE EXCEPTION 'allocation identity cannot change' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER allocation_identity_guard BEFORE UPDATE ON vmid_allocation FOR EACH ROW EXECUTE FUNCTION immutable_allocation_identity();

CREATE FUNCTION guard_template_allocation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM vmid_allocation WHERE id=NEW.vmid_allocation_id AND kind='TEMPLATE' AND released_at IS NULL) THEN
  RAISE EXCEPTION 'template requires held template allocation' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER template_allocation_guard BEFORE INSERT OR UPDATE ON template_version FOR EACH ROW EXECUTE FUNCTION guard_template_allocation();
