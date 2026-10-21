-- Sessions carry the account's session version; raising it (e.g. on a password change) ends
-- every session issued before, on every device.
ALTER TABLE aaptas ADD COLUMN session_version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE admins ADD COLUMN session_version INTEGER NOT NULL DEFAULT 0;
