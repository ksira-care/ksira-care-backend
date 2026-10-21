-- Optimistic-locking versions must never be NULL: Hibernate can't update such a row. Rows added
-- straight with SQL (e.g. demo or admin scripts) may have left them out, so fill those in and
-- default the column from now on.
UPDATE bookings SET version = 0 WHERE version IS NULL;
ALTER TABLE bookings ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE bookings ALTER COLUMN version SET NOT NULL;

UPDATE slots SET version = 0 WHERE version IS NULL;
ALTER TABLE slots ALTER COLUMN version SET DEFAULT 0;
ALTER TABLE slots ALTER COLUMN version SET NOT NULL;
