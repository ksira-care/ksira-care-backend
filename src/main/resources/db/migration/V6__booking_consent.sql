-- When the client accepted the terms on the booking form; null for bookings made before it was recorded.
ALTER TABLE bookings ADD COLUMN consent_given_at TIMESTAMP(6);
