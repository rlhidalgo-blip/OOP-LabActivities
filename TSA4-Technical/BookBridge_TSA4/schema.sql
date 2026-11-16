CREATE TABLE members (
    member_no INTEGER PRIMARY KEY,
    member_id TEXT NOT NULL UNIQUE CHECK (length(trim(member_id)) > 0),
    member_name TEXT NOT NULL CHECK (length(trim(member_name)) > 0),
    membership_type TEXT NOT NULL CHECK (membership_type IN ('STUDENT','FACULTY','GUEST')),
    created_at TEXT NOT NULL
);
CREATE TABLE books (
    book_no INTEGER PRIMARY KEY,
    isbn TEXT NOT NULL UNIQUE CHECK (length(trim(isbn)) > 0),
    title TEXT NOT NULL CHECK (length(trim(title)) > 0),
    created_at TEXT NOT NULL
);
CREATE TABLE loans (
    loan_no INTEGER PRIMARY KEY,
    loan_id TEXT NOT NULL UNIQUE CHECK (length(trim(loan_id)) > 0),
    member_id TEXT NOT NULL REFERENCES members(member_id) ON DELETE RESTRICT,
    isbn TEXT NOT NULL REFERENCES books(isbn) ON DELETE RESTRICT,
    issue_date TEXT NOT NULL,
    due_date TEXT NOT NULL CHECK (due_date > issue_date),
    return_date TEXT,
    status TEXT NOT NULL CHECK (status IN ('ACTIVE','RETURNED')),
    created_at TEXT NOT NULL,
    returned_at TEXT,
    CHECK ((status = 'ACTIVE' AND return_date IS NULL AND returned_at IS NULL)
        OR (status = 'RETURNED' AND return_date IS NOT NULL AND returned_at IS NOT NULL
            AND return_date >= issue_date))
);
CREATE UNIQUE INDEX one_active_loan_per_book ON loans(isbn) WHERE status = 'ACTIVE';
CREATE TABLE reservations (
    queue_no INTEGER PRIMARY KEY,
    reservation_id TEXT NOT NULL UNIQUE CHECK (length(trim(reservation_id)) > 0),
    member_id TEXT NOT NULL REFERENCES members(member_id) ON DELETE RESTRICT,
    isbn TEXT NOT NULL REFERENCES books(isbn) ON DELETE RESTRICT,
    requested_days INTEGER NOT NULL CHECK (requested_days BETWEEN 1 AND 30),
    status TEXT NOT NULL CHECK (status IN ('PENDING','FULFILLED','CANCELLED')),
    created_at TEXT NOT NULL,
    closed_at TEXT,
    CHECK ((status = 'PENDING' AND closed_at IS NULL)
        OR (status <> 'PENDING' AND closed_at IS NOT NULL))
);
CREATE UNIQUE INDEX one_pending_reservation_per_member_book
    ON reservations(member_id, isbn) WHERE status = 'PENDING';
CREATE INDEX pending_reservation_order ON reservations(queue_no) WHERE status = 'PENDING';
