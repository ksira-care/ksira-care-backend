-- Admins allocate aaptas to bookings: bookings arrive without one, and every change is recorded.

ALTER TABLE bookings ALTER COLUMN aapta_id DROP NOT NULL;
ALTER TABLE bookings ADD COLUMN bcc_email_sent BOOLEAN NOT NULL DEFAULT FALSE;

-- Admin "Today / Tomorrow" view: all bookings in a day, whoever holds them.
CREATE INDEX idx_booking_start ON bookings (start_time);
-- Admin "which aaptas are free at this hour" lookup.
CREATE INDEX idx_slots_time_status ON slots (slot_time, status);

CREATE TABLE booking_events (
    id                UUID         PRIMARY KEY,
    booking_id        UUID         NOT NULL REFERENCES bookings (id),
    type              VARCHAR(32)  NOT NULL,
    actor_role        VARCHAR(16)  NOT NULL,
    actor_id          UUID         NOT NULL,
    previous_aapta_id UUID         REFERENCES aaptas (id),
    aapta_id          UUID         REFERENCES aaptas (id),
    occurred_at       TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_booking_events_booking ON booking_events (booking_id, occurred_at);
