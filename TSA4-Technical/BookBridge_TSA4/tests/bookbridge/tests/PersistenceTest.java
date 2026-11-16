package bookbridge.tests;

import bookbridge.exception.DuplicateEntityException;
import bookbridge.exception.ItemUnavailableException;
import bookbridge.exception.LoanLimitExceededException;
import bookbridge.exception.PersistenceException;
import bookbridge.model.FacultyMember;
import bookbridge.model.GuestMember;
import bookbridge.model.LibraryBook;
import bookbridge.model.LoanRecord;
import bookbridge.model.StudentMember;
import bookbridge.persistence.Database;
import bookbridge.service.PersistentLibraryService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

/** Real SQLite integration checks; every case gets an isolated database. */
public final class PersistenceTest {
    private static final Path SCHEMA = Path.of("schema.sql");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-16T04:00:00Z"), ZoneOffset.UTC);
    private static int passed, failed;
    private PersistenceTest() { }

    public static void main(String[] args) throws Exception {
        run("P01 first-run seed once", PersistenceTest::seedOnce);
        run("P02 register and search-safe literal text", PersistenceTest::registerLiteralText);
        run("P03 duplicate and duration rollback", PersistenceTest::rejectedMutations);
        run("P04 issue and restore after restart", PersistenceTest::issueRestart);
        run("P05 return and retained loan history", PersistenceTest::returnHistory);
        run("P06 pending and cancelled IDs retained", PersistenceTest::cancelAndIds);
        run("P07 FIFO across different books", PersistenceTest::fifoAcrossBooks);
        run("P08 failed second write rolls back", PersistenceTest::atomicProcessing);
        run("P09 database uniqueness and foreign keys", PersistenceTest::databaseConstraints);
        run("P10 corrupt saved state is rejected", PersistenceTest::corruptState);
        run("P11 reopen and repeated refresh", PersistenceTest::repeatRefresh);
        run("P12 unavailable database recovery", PersistenceTest::outageRecovery);
        run("P13 fulfilled reservation ID survives restart", PersistenceTest::fulfilledId);
        System.out.println("TSA4 persistence tests: " + passed + " passed, " + failed + " failed");
        if (failed != 0) throw new AssertionError("Persistence failures: " + failed);
    }

    private static void seedOnce() throws Exception {
        Fixture f = fixture();
        eq(f.service.refresh().books().size(), 3);
        eq(f.service.refresh().members().size(), 3);
        f.service.initialize();
        eq(f.service.refresh().books().size(), 3);
        eq(f.service.refresh().members().size(), 3);
    }

    private static void registerLiteralText() throws Exception {
        Fixture f = fixture();
        f.service.registerMember(new StudentMember("X'; DROP TABLE books;--", "O'Brien"));
        f.service.addBook(new LibraryBook("ISBN'--", "Collections ' OR 1=1"));
        var snapshot = f.reopen().refresh();
        check(snapshot.members().stream().anyMatch(m -> m.getMemberName().equals("O'Brien")));
        check(snapshot.books().stream().anyMatch(b -> b.title().equals("Collections ' OR 1=1")));
        eq(snapshot.books().size(), 4);
    }

    private static void rejectedMutations() throws Exception {
        Fixture f = fixture();
        expect(DuplicateEntityException.class, () -> f.service.addBook(new LibraryBook("9780000000001", "Duplicate")));
        expect(LoanLimitExceededException.class, () -> f.service.issueLoan("G100", "9780000000001", 8));
        eq(f.reopen().refresh().loans().size(), 0);
        eq(f.reopen().refresh().books().size(), 3);
    }

    private static void issueRestart() throws Exception {
        Fixture f = fixture();
        LoanRecord loan = f.service.issueLoan("S100", "9780000000001", 14).value();
        var snapshot = f.reopen().refresh();
        eq(snapshot.loans().get(0).loanId(), loan.loanId());
        eq(snapshot.loans().get(0).dueDate(), loan.dueDate());
        eq(snapshot.summary().checkedOutBooks(), 1);
        expect(ItemUnavailableException.class, () -> f.reopen().issueLoan("F100", "9780000000001", 1));
    }

    private static void returnHistory() throws Exception {
        Fixture f = fixture();
        String id = f.service.issueLoan("S100", "9780000000001", 1).value().loanId();
        f.reopen().returnBook(id);
        var snapshot = f.reopen().refresh();
        eq(snapshot.summary().availableBooks(), 3);
        eq(snapshot.summary().historicalLoans(), 1);
        check(snapshot.loans().get(0).isReturned());
        expect(java.util.NoSuchElementException.class, () -> f.reopen().returnBook(id));
    }

    private static void cancelAndIds() throws Exception {
        Fixture f = fixture();
        f.service.issueLoan("S100", "9780000000001", 1);
        String id = f.service.reserveBook("F100", "9780000000001", 2).value().reservationId();
        f.reopen().cancelReservation(id);
        eq(f.reopen().refresh().reservations().size(), 0);
        try (Connection c = f.database.open(); PreparedStatement q = c.prepareStatement(
                "SELECT status FROM reservations WHERE reservation_id=?")) {
            q.setString(1, id);
            try (ResultSet r = q.executeQuery()) { check(r.next() && r.getString(1).equals("CANCELLED")); }
        }
        PersistentLibraryService sameId = new PersistentLibraryService(f.database, CLOCK, () -> id.substring(2));
        expect(DuplicateEntityException.class, () -> sameId.reserveBook("F100", "9780000000001", 1));
    }

    private static void fifoAcrossBooks() throws Exception {
        Fixture f = fixture();
        f.service.issueLoan("S100", "9780000000001", 1);
        f.service.issueLoan("S100", "9780000000002", 1);
        String a = f.service.reserveBook("F100", "9780000000001", 2).value().reservationId();
        String b = f.service.reserveBook("G100", "9780000000001", 2).value().reservationId();
        f.service.reserveBook("G100", "9780000000002", 2);
        f.service.returnBook(f.service.refresh().loans().get(1).loanId());
        eq(f.reopen().processNextReservation().value().orElseThrow().isbn(), "9780000000002");
        eq(f.reopen().refresh().reservations().get(0).reservationId(), a);
        f.service.returnBook(f.service.refresh().loans().get(0).loanId());
        eq(f.reopen().processNextReservation().value().orElseThrow().member().getMemberId(), "F100");
        eq(f.reopen().refresh().reservations().get(0).reservationId(), b);
    }

    private static void atomicProcessing() throws Exception {
        Fixture f = fixture();
        String loanId = f.service.issueLoan("S100", "9780000000001", 1).value().loanId();
        String reservationId = f.service.reserveBook("F100", "9780000000001", 2).value().reservationId();
        f.service.returnBook(loanId);
        try (Connection c = f.database.open(); PreparedStatement p = c.prepareStatement(
                "CREATE TRIGGER fail_close BEFORE UPDATE ON reservations BEGIN SELECT RAISE(ABORT,'forced'); END")) {
            p.execute();
        }
        expect(PersistenceException.class, () -> f.reopen().processNextReservation());
        eq(f.reopen().refresh().loans().size(), 1);
        eq(f.reopen().refresh().reservations().get(0).reservationId(), reservationId);
    }

    private static void databaseConstraints() throws Exception {
        Fixture f = fixture();
        f.service.issueLoan("S100", "9780000000001", 1);
        try (Connection c = f.database.open()) {
            try (PreparedStatement p = c.prepareStatement("INSERT INTO loans(loan_id,member_id,isbn,issue_date,due_date,status,created_at) VALUES('X','F100','9780000000001','2026-11-16','2026-11-17','ACTIVE','now')")) {
                expect(java.sql.SQLException.class, p::executeUpdate);
            }
            try (PreparedStatement p = c.prepareStatement("INSERT INTO reservations(reservation_id,member_id,isbn,requested_days,status,created_at) VALUES('X','MISSING','9780000000001',1,'PENDING','now')")) {
                expect(java.sql.SQLException.class, p::executeUpdate);
            }
        }
    }

    private static void corruptState() throws Exception {
        Fixture f = fixture();
        // An overlong saved guest loan violates the original member policy.
        try (Connection c = f.database.open(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO loans(loan_id,member_id,isbn,issue_date,due_date,status,created_at) VALUES('BAD','G100','9780000000001','2026-11-01','2026-11-30','ACTIVE','now')")) {
            p.executeUpdate();
        }
        expect(PersistenceException.class, () -> f.reopen().refresh());
    }

    private static void repeatRefresh() throws Exception {
        Fixture f = fixture();
        for (int i = 0; i < 10; i++) {
            eq(f.reopen().refresh().books().size(), 3);
            eq(f.reopen().refresh().summary().historicalLoans(), 0);
        }
    }

    private static void outageRecovery() throws Exception {
        Fixture f = fixture();
        Path file = f.directory.resolve("library.db");
        Path backup = f.directory.resolve("library-backup.db");
        Files.move(file, backup);
        Files.createDirectory(file);
        try {
            expect(PersistenceException.class, () -> f.service.refresh());
        } finally {
            Files.delete(file);
            Files.move(backup, file);
        }
        eq(f.service.refresh().books().size(), 3);
    }

    private static void fulfilledId() throws Exception {
        Fixture f = fixture();
        String loan = f.service.issueLoan("S100", "9780000000001", 1).value().loanId();
        String reservation = f.service.reserveBook("F100", "9780000000001", 2).value().reservationId();
        f.service.returnBook(loan);
        f.reopen().processNextReservation();
        eq(f.reopen().refresh().reservations().size(), 0);
        PersistentLibraryService sameId = new PersistentLibraryService(f.database, CLOCK,
                () -> reservation.substring(2));
        expect(DuplicateEntityException.class, () -> sameId.reserveBook("G100", "9780000000001", 1));
    }

    private static Fixture fixture() throws Exception {
        Path dir = Files.createTempDirectory("bookbridge-tsa4-");
        Database db = new Database(dir.resolve("library.db"), SCHEMA);
        db.initialize();
        AtomicInteger next = new AtomicInteger();
        return new Fixture(dir, db, new PersistentLibraryService(db, CLOCK,
                () -> "test-" + next.incrementAndGet()));
    }

    private record Fixture(Path directory, Database database, PersistentLibraryService service) {
        PersistentLibraryService reopen() {
            return new PersistentLibraryService(database, CLOCK,
                    () -> java.util.UUID.randomUUID().toString());
        }
    }

    private static void run(String name, Checked test) {
        try { test.run(); passed++; System.out.println("PASS " + name); }
        catch (Throwable error) { failed++; System.out.println("FAIL " + name + ": " + error); error.printStackTrace(); }
    }
    private static void eq(Object actual, Object expected) {
        if (!java.util.Objects.equals(actual, expected)) throw new AssertionError(actual + " != " + expected);
    }
    private static void check(boolean result) { if (!result) throw new AssertionError("Check failed"); }
    private static void expect(Class<? extends Throwable> kind, Checked task) {
        try { task.run(); } catch (Throwable error) {
            if (kind.isInstance(error)) {
                System.out.println("  EXPECTED " + error.getClass().getSimpleName() + ": " + error.getMessage());
                return;
            }
            throw new AssertionError("Unexpected error", error);
        }
        throw new AssertionError("Expected " + kind.getSimpleName());
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
}
