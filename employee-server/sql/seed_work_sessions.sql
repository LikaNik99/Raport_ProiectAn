-- Test data for work_sessions (populate HR statistics)
-- Run this AFTER schema.sql and seed.sql have been applied
-- Usage: psql -U postgres -d uzina_db -f seed_work_sessions.sql

-- Clear existing work_sessions (optional, comment out if you want to keep them)
-- DELETE FROM work_sessions;

-- Ion (WORKER, ID=1001) work sessions for past 10 days
INSERT INTO work_sessions (user_id, work_date, start_time, end_time, status) VALUES
(1001, '2025-12-01', '2025-12-01 08:05:00', '2025-12-01 17:10:00', 'COMPLETED'),
(1001, '2025-12-02', '2025-12-02 08:03:00', '2025-12-02 17:05:00', 'COMPLETED'),
(1001, '2025-12-03', '2025-12-03 08:10:00', '2025-12-03 17:15:00', 'COMPLETED'),
(1001, '2025-12-04', '2025-12-04 08:02:00', '2025-12-04 17:08:00', 'COMPLETED'),
(1001, '2025-12-05', '2025-12-05 08:08:00', '2025-12-05 17:12:00', 'COMPLETED'),
-- Ion skips 2025-12-06 (Saturday - non-working day)
-- Ion skips 2025-12-07 (Sunday - non-working day)
(1001, '2025-12-08', '2025-12-08 08:04:00', '2025-12-08 17:09:00', 'COMPLETED'),
(1001, '2025-12-09', '2025-12-09 08:06:00', '2025-12-09 17:11:00', 'COMPLETED'),
(1001, '2025-12-10', '2025-12-10 08:01:00', '2025-12-10 17:00:00', 'COMPLETED');

-- Maria (TEAMLEADER, ID=1002) work sessions for past 10 days
INSERT INTO work_sessions (user_id, work_date, start_time, end_time, status) VALUES
(1002, '2025-12-01', '2025-12-01 08:00:00', '2025-12-01 17:30:00', 'COMPLETED'),
(1002, '2025-12-02', '2025-12-02 08:00:00', '2025-12-02 17:30:00', 'COMPLETED'),
(1002, '2025-12-03', '2025-12-03 08:00:00', '2025-12-03 17:30:00', 'COMPLETED'),
(1002, '2025-12-04', '2025-12-04 08:00:00', '2025-12-04 17:30:00', 'COMPLETED'),
(1002, '2025-12-05', '2025-12-05 08:00:00', '2025-12-05 17:30:00', 'COMPLETED'),
(1002, '2025-12-08', '2025-12-08 08:00:00', '2025-12-08 17:30:00', 'COMPLETED'),
(1002, '2025-12-09', '2025-12-09 08:00:00', '2025-12-09 17:30:00', 'COMPLETED'),
(1002, '2025-12-10', '2025-12-10 08:00:00', '2025-12-10 17:30:00', 'COMPLETED');

-- Optional: Add some leave requests so HR sees leave data
INSERT INTO leave_requests (user_id, date_from, date_to, reason, status) VALUES
(1001, '2025-12-15', '2025-12-16', 'Sick leave', 'APPROVED'),
(1002, '2025-12-20', '2025-12-22', 'Vacation', 'PENDING');

-- Show counts
SELECT 'work_sessions count:' as info, COUNT(*) as count FROM work_sessions;
SELECT 'leave_requests count:' as info, COUNT(*) as count FROM leave_requests;
