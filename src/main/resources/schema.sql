CREATE TABLE IF NOT EXISTS vendor (
    id   INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL
);

-- Also enforces uniqueness on databases created before this constraint was added.
CREATE UNIQUE INDEX IF NOT EXISTS vendor_name_unique ON vendor(name);

CREATE TABLE IF NOT EXISTS offer (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    vendor_id   INTEGER NOT NULL REFERENCES vendor(id),
    currency    TEXT NOT NULL,
    amount      REAL NOT NULL,
    description TEXT
);
