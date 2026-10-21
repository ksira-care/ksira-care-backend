-- Some booking changes are made by the system itself (e.g. an unmarked session becoming an aapta
-- no-show at the end of its day), so an event may have no acting account.
ALTER TABLE booking_events ALTER COLUMN actor_id DROP NOT NULL;
