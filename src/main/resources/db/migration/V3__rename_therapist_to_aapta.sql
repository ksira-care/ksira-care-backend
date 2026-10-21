-- "Therapist" is now "aapta" across the product. Renames in place so existing data is kept.
-- Portable across PostgreSQL (production) and H2 in PostgreSQL mode (development).

ALTER TABLE therapists RENAME TO aaptas;
ALTER TABLE aaptas RENAME CONSTRAINT uk_therapists_email TO uk_aaptas_email;

ALTER TABLE therapist_languages RENAME TO aapta_languages;
ALTER TABLE aapta_languages RENAME COLUMN therapist_id TO aapta_id;

ALTER TABLE slots RENAME COLUMN therapist_id TO aapta_id;
ALTER TABLE slots RENAME CONSTRAINT uk_therapist_slot_time TO uk_aapta_slot_time;

ALTER TABLE bookings RENAME COLUMN therapist_id TO aapta_id;
ALTER TABLE bookings RENAME COLUMN therapist_fee TO aapta_fee;
ALTER INDEX idx_booking_therapist_start RENAME TO idx_booking_aapta_start;

-- Enum values are stored by name.
UPDATE slots SET status = 'AAPTA_AVAILABLE' WHERE status = 'THERAPIST_AVAILABLE';
UPDATE slots SET status = 'AAPTA_UNAVAILABLE' WHERE status = 'THERAPIST_UNAVAILABLE';
UPDATE bookings SET status = 'AAPTA_NO_SHOW' WHERE status = 'THERAPIST_NO_SHOW';
