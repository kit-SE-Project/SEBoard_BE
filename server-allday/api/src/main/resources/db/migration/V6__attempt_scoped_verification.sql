ALTER TABLE return_verification DROP CONSTRAINT return_verification_job_id_verified_at_key;
ALTER TABLE return_verification ADD COLUMN attempt integer NOT NULL DEFAULT 0 CHECK (attempt >= 0);
ALTER TABLE return_verification ADD CONSTRAINT verification_one_per_attempt UNIQUE (job_id,attempt);
ALTER TABLE job ADD COLUMN credential_step_hash text;

CREATE OR REPLACE FUNCTION guard_returned() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.status='RETURNED' AND OLD.status<>'RETURNED' THEN
  IF OLD.status<>'RETURNING' OR NOT EXISTS (
   SELECT 1 FROM return_verification v JOIN job j ON j.id=v.job_id
   WHERE v.rental_id=NEW.id AND j.rental_id=NEW.id AND j.type='RETURN'
    AND j.desired_revision=NEW.desired_revision AND j.status='SUCCEEDED'
    AND v.result='PASS' AND v.attempt=j.retry_count
    AND (SELECT count(*) FROM verification_item i WHERE i.verification_id=v.id)=5
    AND NOT EXISTS (SELECT 1 FROM verification_item i WHERE i.verification_id=v.id AND i.passed IS DISTINCT FROM true)
  ) THEN RAISE EXCEPTION 'RETURNED requires complete current return verification' USING ERRCODE='23514'; END IF;
 END IF;
 RETURN NEW;
END $$;
