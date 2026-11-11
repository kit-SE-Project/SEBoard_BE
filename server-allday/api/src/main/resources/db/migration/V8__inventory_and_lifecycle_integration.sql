ALTER TABLE server_node ADD COLUMN observed_status text NOT NULL DEFAULT 'UNKNOWN'
 CHECK (observed_status IN ('AVAILABLE','MAINTENANCE','QUARANTINED','UNKNOWN'));
ALTER TABLE server_node ADD COLUMN vmid_start integer;
ALTER TABLE server_node ADD COLUMN vmid_end integer;
ALTER TABLE server_node ADD COLUMN ip_pool_start inet;
ALTER TABLE server_node ADD COLUMN ip_pool_end inet;
ALTER TABLE server_node ADD CONSTRAINT vmid_pool_bounds CHECK
 ((vmid_start IS NULL AND vmid_end IS NULL) OR
  (vmid_start IS NOT NULL AND vmid_end IS NOT NULL AND vmid_start>=100 AND vmid_end<=999999999
   AND vmid_end>=vmid_start AND vmid_end-vmid_start<=100000));
ALTER TABLE server_node ADD CONSTRAINT ip_pool_bounds CHECK
 ((ip_pool_start IS NULL AND ip_pool_end IS NULL) OR
  (ip_pool_start IS NOT NULL AND ip_pool_end IS NOT NULL AND vm_subnet IS NOT NULL
   AND family(ip_pool_start)=4 AND family(ip_pool_end)=4 AND family(vm_subnet)=4
   AND masklen(ip_pool_start)=32 AND masklen(ip_pool_end)=32
   AND ip_pool_start<<=vm_subnet AND ip_pool_end<<=vm_subnet
   AND ip_pool_start>network(vm_subnet)::inet AND ip_pool_end<broadcast(vm_subnet)
   AND ip_pool_end>=ip_pool_start AND ip_pool_end-ip_pool_start<=4095));
ALTER TABLE template_version ADD COLUMN approved_by bigint REFERENCES external_subject_binding(member_id);
ALTER TABLE template_version ADD COLUMN approval_reason text;
CREATE INDEX rental_lifecycle_idx ON rental(server_id,id)
 WHERE status NOT IN ('RETURNED','CANCELLED');
