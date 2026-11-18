package bookbridge.service;

import bookbridge.exception.PersistenceException;
import bookbridge.model.LibraryBook;
import bookbridge.model.LibrarySnapshot;
import bookbridge.model.LoanRecord;
import bookbridge.model.Member;
import bookbridge.model.Reservation;
import bookbridge.persistence.Database;
import bookbridge.persistence.LibraryDao;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/** Loads one validated service per transaction and publishes results after commit. */
public final class PersistentLibraryService {
    private final Database database;
    private final Clock clock;
    private final Supplier<String> ids;

    public record Result<T>(T value, LibrarySnapshot snapshot) { }

    @FunctionalInterface
    private interface Work<T> {
        T run(LibraryService service, LibraryDao dao) throws SQLException;
    }

    public PersistentLibraryService(Database database) {
        this(database, Clock.systemDefaultZone(), () -> java.util.UUID.randomUUID().toString());
    }

    public PersistentLibraryService(Database database, Clock clock, Supplier<String> ids) {
        this.database = Objects.requireNonNull(database);
        this.clock = Objects.requireNonNull(clock);
        this.ids = Objects.requireNonNull(ids);
    }

    public void initialize() { database.initialize(); }

    public LibrarySnapshot refresh() { return execute((service, dao) -> null).snapshot(); }

    public Result<Member> registerMember(Member member) {
        return execute((service, dao) -> {
            Member saved = service.registerMember(member);
            dao.insertMember(saved);
            return saved;
        });
    }

    public Result<LibraryBook> addBook(LibraryBook book) {
        return execute((service, dao) -> {
            LibraryBook saved = service.addBook(book);
            dao.insertBook(saved);
            return saved;
        });
    }

    public Result<LoanRecord> issueLoan(String memberId, String isbn, int days) {
        return execute((service, dao) -> {
            LoanRecord loan = service.issueLoan(memberId, isbn, days);
            dao.insertLoan(loan);
            return loan;
        });
    }

    public Result<LoanRecord> returnBook(String loanId) {
        return execute((service, dao) -> {
            LoanRecord loan = service.returnBook(loanId);
            dao.returnLoan(loan);
            return loan;
        });
    }

    public Result<Reservation> reserveBook(String memberId, String isbn, int days) {
        return execute((service, dao) -> {
            Reservation reservation = service.reserveBook(memberId, isbn, days);
            dao.insertReservation(reservation);
            return reservation;
        });
    }

    public Result<Reservation> cancelReservation(String reservationId) {
        return execute((service, dao) -> {
            Reservation reservation = service.cancelReservation(reservationId);
            dao.closeReservation(reservation.reservationId(), "CANCELLED");
            return reservation;
        });
    }

    public Result<Optional<LoanRecord>> processNextReservation() {
        return execute((service, dao) -> {
            List<Reservation> before = service.listPendingReservations();
            Optional<LoanRecord> result = service.processNextReservation();
            if (result.isPresent()) {
                LoanRecord loan = result.orElseThrow();
                Reservation first = before.stream().filter(r -> r.isbn().equals(loan.isbn()))
                        .findFirst().orElseThrow();
                if (!first.memberId().equals(loan.member().getMemberId())) {
                    throw new IllegalStateException("Reservation order changed unexpectedly.");
                }
                dao.insertLoan(loan);
                dao.closeReservation(first.reservationId(), "FULFILLED");
            }
            return result;
        });
    }

    private <T> Result<T> execute(Work<T> work) {
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                LibraryDao dao = new LibraryDao(connection);
                LibraryService service = new LibraryService(clock, ids);
                try {
                    service.restore(dao.load());
                } catch (RuntimeException corrupt) {
                    throw new PersistenceException("Saved library records are invalid.", corrupt);
                }
                T value = work.run(service, dao);
                LibrarySnapshot snapshot = new LibrarySnapshot(service.listMembers(), service.listBooks(),
                        service.listLoanHistory(), service.listPendingReservations(),
                        service.getSortedLoans(), service.getCirculationSummary());
                connection.commit();
                return new Result<>(value, snapshot);
            } catch (SQLException | RuntimeException error) {
                try { connection.rollback(); } catch (SQLException rollback) { error.addSuppressed(rollback); }
                if (error instanceof SQLException sql) {
                    throw new PersistenceException("Database operation failed.", sql);
                }
                throw error;
            }
        } catch (SQLException error) {
            throw new PersistenceException("Could not open BookBridge database.", error);
        }
    }
}
