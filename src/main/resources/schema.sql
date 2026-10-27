DROP TABLE IF EXISTS vendor;
DROP TABLE IF EXISTS offer;
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS client;
DROP TABLE IF EXISTS payment;

CREATE TABLE vendor
(
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL
);

CREATE UNIQUE INDEX vendor_name_unique ON vendor (name);

CREATE TABLE offer
(
    id          TEXT PRIMARY KEY,
    name        TEXT    NOT NULL,
    vendor_id   INTEGER NOT NULL REFERENCES vendor (id),
    currency    TEXT    NOT NULL,
    amount      TEXT    NOT NULL,
    description TEXT    NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity >= 0)
);

CREATE TABLE client
(
    id    TEXT PRIMARY KEY,
    name  TEXT NOT NULL,
    email TEXT NOT NULL
);

CREATE TABLE reservation
(
    id                      TEXT PRIMARY KEY,
    version                 INTEGER NOT NULL,
    client_id               TEXT    NOT NULL REFERENCES client (id),
    offer_items_data        TEXT    NOT NULL, -- JSON dump
    create_timestamp        INTEGER NOT NULL,
    last_modified_timestamp INTEGER NOT NULL
);

CREATE TABLE payment
(
    id                TEXT PRIMARY KEY,
    reservation_id    TEXT    NOT NULL REFERENCES reservation (id),
    amount            TEXT    NOT NULL,
    currency          TEXT    NOT NULL,
    payment_timestamp INTEGER NOT NULL
);
