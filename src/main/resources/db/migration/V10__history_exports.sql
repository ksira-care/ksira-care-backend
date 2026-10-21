-- Every History export (which month, who, when). Bookings older than History's window are only
-- deleted once their month appears here, so nothing is deleted without having been exported.
CREATE TABLE history_exports (
    id           UUID         PRIMARY KEY,
    export_month DATE         NOT NULL,
    admin_id     UUID         NOT NULL REFERENCES admins (id),
    exported_at  TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_history_exports_month ON history_exports (export_month);
