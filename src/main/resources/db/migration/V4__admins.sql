-- Admin accounts: Ksira Care staff who manage aaptas and bookings. Same sign-in columns as aaptas.
CREATE TABLE admins (
    id          UUID         PRIMARY KEY,
    first_name  VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255),
    last_name   VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP(6) NOT NULL,
    updated_at  TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_admins_email UNIQUE (email)
);
