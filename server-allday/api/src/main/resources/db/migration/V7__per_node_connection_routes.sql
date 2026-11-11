ALTER TABLE server_node ADD COLUMN public_ssh_host text;
ALTER TABLE server_node ADD COLUMN public_ssh_port integer CHECK (public_ssh_port BETWEEN 1 AND 65535);
ALTER TABLE server_node ADD CONSTRAINT public_ssh_host_format CHECK (public_ssh_host IS NULL OR public_ssh_host ~ '^[A-Za-z0-9.-]+$');
