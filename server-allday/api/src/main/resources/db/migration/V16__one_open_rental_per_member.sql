CREATE INDEX rental_open_member_idx ON rental(member_id)
 WHERE status NOT IN ('RETURNED', 'CANCELLED');

-- Keep pre-existing contracts intact. Members with multiple open contracts can
-- finish them, but cannot take out a new contract until all of them are closed.
CREATE FUNCTION enforce_one_open_rental_per_member() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.status IN ('RETURNED', 'CANCELLED') THEN
        RETURN NEW;
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.member_id = NEW.member_id
       AND OLD.status NOT IN ('RETURNED', 'CANCELLED') THEN
        RETURN NEW;
    END IF;

    -- A row write serializes concurrent requests even at higher isolation levels.
    UPDATE external_subject_binding SET last_checked_at = last_checked_at WHERE member_id = NEW.member_id;
    IF EXISTS (SELECT 1 FROM rental
               WHERE member_id = NEW.member_id AND status NOT IN ('RETURNED', 'CANCELLED')) THEN
        RAISE EXCEPTION 'Member already has an open rental' USING ERRCODE = 'P1010';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER rental_one_open_per_member
    BEFORE INSERT OR UPDATE OF member_id, status ON rental
    FOR EACH ROW EXECUTE FUNCTION enforce_one_open_rental_per_member();
