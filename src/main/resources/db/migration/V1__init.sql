-- Users
CREATE TABLE users (
    id          UUID PRIMARY KEY,
    full_name   VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- Venues
CREATE TABLE venues (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    city        VARCHAR(255) NOT NULL,
    capacity    INTEGER NOT NULL
);

-- Events
CREATE TABLE events (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    venue_id    UUID NOT NULL,
    starts_at   TIMESTAMPTZ NOT NULL,
    status      VARCHAR(20) NOT NULL,
    CONSTRAINT fk_events_venue FOREIGN KEY (venue_id) REFERENCES venues (id)
);

CREATE INDEX idx_events_venue_id ON events (venue_id);

-- Seats
CREATE TABLE seats (
    id          UUID PRIMARY KEY,
    event_id    UUID NOT NULL,
    seat_label  VARCHAR(20) NOT NULL,
    price       NUMERIC(10, 2) NOT NULL,
    CONSTRAINT fk_seats_event FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT uq_seats_event_label UNIQUE (event_id, seat_label)
);

CREATE INDEX idx_seats_event_id ON seats (event_id);

-- Bookings
CREATE TABLE bookings (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_bookings_user_id ON bookings (user_id);

-- Booking-Seat join entity
CREATE TABLE booking_seats (
    id              UUID PRIMARY KEY,
    booking_id      UUID NOT NULL,
    seat_id         UUID NOT NULL,
    status          VARCHAR(20) NOT NULL,
    hold_expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_booking_seats_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_booking_seats_seat FOREIGN KEY (seat_id) REFERENCES seats (id)
);

CREATE INDEX idx_booking_seats_booking_id ON booking_seats (booking_id);
CREATE INDEX idx_booking_seats_seat_id ON booking_seats (seat_id);

-- Payments
CREATE TABLE payments (
    id          UUID PRIMARY KEY,
    booking_id  UUID NOT NULL,
    amount      NUMERIC(10, 2) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT uq_payments_booking UNIQUE (booking_id)
);