package bookbridge.persistence;

import bookbridge.model.BookStatus;
import bookbridge.model.FacultyMember;
import bookbridge.model.GuestMember;
import bookbridge.model.LibraryBook;
import bookbridge.model.LibraryState;
import bookbridge.model.LoanRecord;
import bookbridge.model.Member;
import bookbridge.model.MembershipType;
import bookbridge.model.Reservation;
import bookbridge.model.StudentMember;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** SQL for four tables. The caller owns the connection and transaction. */
public final class LibraryDao {
    private final Connection connection;
    public LibraryDao(Connection connection) { this.connection = connection; }

    public LibraryState load() throws SQLException {
        Map<String, Member> members = new LinkedHashMap<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT member_id, member_name, membership_type FROM members ORDER BY member_no");
                ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
                String id = rows.getString(1), name = rows.getString(2);
                Member member = switch (MembershipType.valueOf(rows.getString(3))) {
                    case STUDENT -> new StudentMember(id, name);
                    case FACULTY -> new FacultyMember(id, name);
                    case GUEST -> new GuestMember(id, name);
                };
                members.put(id, member);
            }
        }
        List<LibraryBook> books = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT b.isbn, b.title, CASE WHEN l.loan_id IS NULL THEN 'AVAILABLE' "
                + "ELSE 'CHECKED_OUT' END FROM books b LEFT JOIN loans l "
                + "ON l.isbn=b.isbn AND l.status='ACTIVE' ORDER BY b.book_no");
                ResultSet rows = query.executeQuery()) {
            while (rows.next()) books.add(new LibraryBook(rows.getString(1), rows.getString(2),
                    BookStatus.valueOf(rows.getString(3))));
        }
        List<LoanRecord> loans = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT loan_id, member_id, isbn, issue_date, due_date, return_date, status FROM loans ORDER BY loan_no");
                ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
                Member member = members.get(rows.getString(2));
                if (member == null) throw new SQLException("Loan refers to a missing member.");
                String returned = rows.getString(6);
                LoanRecord loan = new LoanRecord(rows.getString(1), member, rows.getString(3),
                        LocalDate.parse(rows.getString(4)), LocalDate.parse(rows.getString(5)),
                        returned == null ? null : LocalDate.parse(returned));
                if (!loan.status().name().equals(rows.getString(7))) {
                    throw new SQLException("Loan status disagrees with its return date.");
                }
                loans.add(loan);
            }
        }
        List<Reservation> pending = new ArrayList<>();
        List<String> closedIds = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT reservation_id, member_id, isbn, requested_days, status FROM reservations ORDER BY queue_no");
                ResultSet rows = query.executeQuery()) {
            while (rows.next()) {
                String id = rows.getString(1);
                if ("PENDING".equals(rows.getString(5))) {
                    pending.add(new Reservation(id, rows.getString(2), rows.getString(3), rows.getInt(4)));
                } else if ("CANCELLED".equals(rows.getString(5))
                        || "FULFILLED".equals(rows.getString(5))) {
                    Member member = members.get(rows.getString(2));
                    String isbn = rows.getString(3);
                    if (member == null || books.stream().noneMatch(b -> b.isbn().equals(isbn))
                            || rows.getInt(4) > member.getMaximumLoanDays()) {
                        throw new SQLException("Closed reservation has invalid references or duration: " + id);
                    }
                    closedIds.add(id);
                } else {
                    throw new SQLException("Unknown reservation status: " + id);
                }
            }
        }
        return new LibraryState(List.copyOf(members.values()), books, loans, pending, closedIds);
    }

    public void insertMember(Member member) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "INSERT INTO members(member_id,member_name,membership_type,created_at) VALUES(?,?,?,?)")) {
            update.setString(1, member.getMemberId());
            update.setString(2, member.getMemberName());
            update.setString(3, member.getMembershipType().name());
            update.setString(4, Instant.now().toString());
            changed(update);
        }
    }

    public void insertBook(LibraryBook book) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "INSERT INTO books(isbn,title,created_at) VALUES(?,?,?)")) {
            update.setString(1, book.isbn());
            update.setString(2, book.title());
            update.setString(3, Instant.now().toString());
            changed(update);
        }
    }

    public void insertLoan(LoanRecord loan) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "INSERT INTO loans(loan_id,member_id,isbn,issue_date,due_date,status,created_at) "
                + "VALUES(?,?,?,?,?,'ACTIVE',?)")) {
            update.setString(1, loan.loanId());
            update.setString(2, loan.member().getMemberId());
            update.setString(3, loan.isbn());
            update.setString(4, loan.issueDate().toString());
            update.setString(5, loan.dueDate().toString());
            update.setString(6, Instant.now().toString());
            changed(update);
        }
    }

    public void returnLoan(LoanRecord loan) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE loans SET return_date=?, status='RETURNED', returned_at=? "
                + "WHERE loan_id=? AND status='ACTIVE'")) {
            update.setString(1, loan.returnDate().toString());
            update.setString(2, Instant.now().toString());
            update.setString(3, loan.loanId());
            changed(update);
        }
    }

    public void insertReservation(Reservation reservation) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "INSERT INTO reservations(reservation_id,member_id,isbn,requested_days,status,created_at) "
                + "VALUES(?,?,?,?,'PENDING',?)")) {
            update.setString(1, reservation.reservationId());
            update.setString(2, reservation.memberId());
            update.setString(3, reservation.isbn());
            update.setInt(4, reservation.requestedDays());
            update.setString(5, Instant.now().toString());
            changed(update);
        }
    }

    public void closeReservation(String id, String status) throws SQLException {
        if (!"CANCELLED".equals(status) && !"FULFILLED".equals(status)) {
            throw new IllegalArgumentException("Invalid closed reservation status.");
        }
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE reservations SET status=?, closed_at=? WHERE reservation_id=? AND status='PENDING'")) {
            update.setString(1, status);
            update.setString(2, Instant.now().toString());
            update.setString(3, id);
            changed(update);
        }
    }

    private static void changed(PreparedStatement update) throws SQLException {
        if (update.executeUpdate() != 1) throw new SQLException("Expected exactly one changed row.");
    }
}
