-- Compare host addresses with /32 masks, independently of the subnet prefix.
ALTER TABLE server_node DROP CONSTRAINT ip_pool_bounds;
ALTER TABLE server_node ADD CONSTRAINT ip_pool_bounds CHECK
 ((ip_pool_start IS NULL AND ip_pool_end IS NULL) OR
  (ip_pool_start IS NOT NULL AND ip_pool_end IS NOT NULL AND vm_subnet IS NOT NULL
   AND family(ip_pool_start)=4 AND family(ip_pool_end)=4 AND family(vm_subnet)=4
   AND masklen(ip_pool_start)=32 AND masklen(ip_pool_end)=32
   AND ip_pool_start<<=vm_subnet AND ip_pool_end<<=vm_subnet
   AND ip_pool_start>host(network(vm_subnet))::inet AND ip_pool_end<host(broadcast(vm_subnet))::inet
   AND ip_pool_end>=ip_pool_start AND ip_pool_end-ip_pool_start<=4095));
