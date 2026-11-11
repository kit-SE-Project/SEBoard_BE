ALTER TABLE category ADD COLUMN deleted_at timestamptz;
ALTER TABLE category DROP CONSTRAINT category_name_key;
CREATE UNIQUE INDEX category_active_name_key ON category(name) WHERE deleted_at IS NULL;
