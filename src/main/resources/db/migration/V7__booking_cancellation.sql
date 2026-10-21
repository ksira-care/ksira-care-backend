-- Admins cancel bookings with a reason, kept in the booking's history.
ALTER TABLE booking_events ADD COLUMN note VARCHAR(500);
