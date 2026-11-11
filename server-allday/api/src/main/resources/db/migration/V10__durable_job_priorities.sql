-- Upgrade jobs created by 0.1.0 before enabling durable START/STOP scheduling.
UPDATE job SET priority=40 WHERE type='PREPARE' AND priority<40;
UPDATE job SET priority=60 WHERE type='RESET_CREDENTIAL' AND priority<60;
UPDATE job SET priority=80 WHERE type='STOP' AND priority<80;
UPDATE job SET priority=100 WHERE type='RETURN' AND priority<100;
