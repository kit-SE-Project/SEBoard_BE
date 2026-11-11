-- Legacy configurations keep requiring GPU passthrough unless explicitly disabled.
ALTER TABLE server_node ADD COLUMN gpu_enabled boolean NOT NULL DEFAULT true;
ALTER TABLE server_node ADD CONSTRAINT no_gpu_has_no_mapping CHECK (gpu_enabled OR gpu_mapping_ref IS NULL);
-- gpu_lease remains the node ownership/generation fence even without a physical GPU.
