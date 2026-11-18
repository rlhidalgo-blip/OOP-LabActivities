CREATE TABLE IF NOT EXISTS students (
    student_id TEXT PRIMARY KEY NOT NULL CHECK (length(trim(student_id)) > 0),
    student_name TEXT NOT NULL CHECK (length(trim(student_name)) > 0)
);

CREATE TABLE IF NOT EXISTS service_request (
    ticket_id TEXT PRIMARY KEY NOT NULL CHECK (length(trim(ticket_id)) > 0),
    student_id TEXT NOT NULL REFERENCES students(student_id),
    category TEXT NOT NULL CHECK (category IN ('ENROLLMENT', 'DOCUMENT', 'PAYMENT', 'ADVISING')),
    priority INTEGER NOT NULL CHECK (priority BETWEEN 1 AND 5),
    concern TEXT NOT NULL CHECK (length(trim(concern)) > 0),
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    queue_no INTEGER NOT NULL UNIQUE CHECK (queue_no > 0),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_request_student ON service_request(student_id);
CREATE INDEX IF NOT EXISTS idx_request_category_status ON service_request(category, status);
CREATE INDEX IF NOT EXISTS idx_request_pending ON service_request(status, queue_no);
