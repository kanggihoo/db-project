INSERT INTO users (id, email, password, name, gender, grade, point_balance, created_at, updated_at)
VALUES (7300, 'phase7-user@example.com', 'pw', 'Phase7 User', 'MALE', 'GOLD', 0, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO point_history (id, user_id, type, amount, balance_after, description, created_at)
VALUES
  (7301, 7300, 'EARN', 100, 100, 'p1', TIMESTAMP '2026-05-27 10:00:00'),
  (7302, 7300, 'USE',  100,   0, 'p2', TIMESTAMP '2026-05-27 10:00:00'),
  (7303, 7300, 'EARN', 200, 200, 'p3', TIMESTAMP '2026-05-27 09:00:00'),
  (7304, 7300, 'USE',  100, 100, 'p4', TIMESTAMP '2026-05-27 08:00:00'),
  (7305, 7300, 'EXPIRE', 50, 50, 'p5', TIMESTAMP '2026-05-27 07:00:00')
ON CONFLICT (id) DO NOTHING;
