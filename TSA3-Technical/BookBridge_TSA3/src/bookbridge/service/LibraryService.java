package bookbridge.service;

import bookbridge.exception.DuplicateEntityException;
import bookbridge.exception.ItemUnavailableException;
import bookbridge.exception.LoanLimitExceededException;
import bookbridge.model.BookStatus;
import bookbridge.model.CirculationSummary;
import bookbridge.model.LibraryBook;
import bookbridge.model.LoanRecord;
import bookbridge.model.Member;
import bookbridge.model.MembershipType;
import bookbridge.model.RequestOutcome;
import bookbridge.model.Reservation;
import bookbridge.util.Lookup;
import bookbridge.util.Validation;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * In-memory, single-threaded TSA3 service. All validation and value construction
 * precede each mutation block. Returned domain values and collection snapshots
 * are immutable. No console, Swing, JDBC, or notification delivery dependencies.
 */
public final class LibraryService {
    private final Map<String, LibraryBook> books = new LinkedHashMap<>();
    private final Map<String, Member> members = new LinkedHashMap<>();
    private final Map<String, LoanRecord> activeLoans = new LinkedHashMap<>();
    private final Queue<Reservation> pendingReservations = new ArrayDeque<>();
    private final Set<String> checkedOutISBNs = new HashSet<>();
    private final List<LoanRecord> loanHistory = new ArrayList<>();
    // Retain generated IDs even after return/cancellation to prevent reuse.
    private final Set<String> usedIds = new HashSet<>();
    private final Clock clock;
    private final Supplier<String> idSource;

    public LibraryService() {
        this(Clock.systemDefaultZone(), () -> UUID.randomUUID().toString());
    }

    public LibraryService(Clock clock) {
        this(clock, () -> UUID.randomUUID().toString());
    }

    public LibraryService(Clock clock, Supplier<String> idSource) {
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
        this.idSource = Objects.requireNonNull(idSource, "ID source is required.");
    }

    public Member registerMember(Member member) {
        Objects.requireNonNull(member, "Member is required.");
        if (members.containsKey(member.getMemberId())) {
            throw new DuplicateEntityException("Member ID already registered: " + member.getMemberId());
        }
        members.put(member.getMemberId(), member);
        return member;
    }

    public Optional<Member> findMember(String memberId) {
        return Optional.ofNullable(members.get(Validation.requiredText(memberId, "Member ID")));
    }

    public List<Member> listMembers() { return List.copyOf(members.values()); }

    public List<Member> filterMembersByMembershipType(MembershipType type) {
        Objects.requireNonNull(type, "Membership type is required.");
        return members.values().stream().filter(m -> m.getMembershipType() == type).toList();
    }

    public LibraryBook addBook(LibraryBook book) {
        Objects.requireNonNull(book, "Book is required.");
        if (books.containsKey(book.isbn())) {
            throw new DuplicateEntityException("ISBN already registered: " + book.isbn());
        }
        if (!book.isAvailable()) {
            throw new IllegalArgumentException("New books must be AVAILABLE; use issueLoan to check out.");
        }
        books.put(book.isbn(), book);
        return book;
    }

    public Optional<LibraryBook> findBookByISBN(String isbn) {
        return Optional.ofNullable(books.get(Validation.requiredText(isbn, "ISBN")));
    }

    public List<LibraryBook> searchBooksByTitle(String keyword) {
        String query = Validation.requiredText(keyword, "Title keyword").toLowerCase(Locale.ROOT);
        return books.values().stream()
                .filter(b -> b.title().toLowerCase(Locale.ROOT).contains(query)).toList();
    }

    public List<LibraryBook> listBooks() { return List.copyOf(books.values()); }

    public List<LibraryBook> filterBooksByStatus(BookStatus status) {
        Objects.requireNonNull(status, "Book status is required.");
        return books.values().stream().filter(b -> b.status() == status).toList();
    }

    /** Classification is a preview: it does not issue a loan or enqueue a reservation. */
    public RequestOutcome classifyRequest(String memberId, String isbn, int requestedDays) {
        Member member = requireMember(memberId);
        LibraryBook book = requireBook(isbn);
        validatePositiveDays(requestedDays);
        if (requestedDays > member.getMaximumLoanDays()) {
            return RequestOutcome.DENIED;
        }
        if (!book.isAvailable() || hasPendingReservation(book.isbn())) {
            return RequestOutcome.RESERVED;
        }
        return RequestOutcome.APPROVED;
    }

    public LoanRecord issueLoan(String memberId, String isbn, int requestedDays) {
        Member member = requireMember(memberId);
        LibraryBook book = requireBook(isbn);
        validateDuration(member, requestedDays);
        if (hasPendingReservation(book.isbn())) {
            throw new ItemUnavailableException("Book has waiting reservations; process the queue: " + book.isbn());
        }
        return createLoan(member, book, requestedDays);
    }

    /** Both callers validate the member's duration before reaching this method. */
    private LoanRecord createLoan(Member member, LibraryBook book, int requestedDays) {
        if (!book.isAvailable() || checkedOutISBNs.contains(book.isbn())) {
            throw new ItemUnavailableException("Book is already checked out: " + book.isbn());
        }
        LocalDate today = LocalDate.now(clock);
        String loanId = freshId("L-");
        LoanRecord loan = new LoanRecord(loanId, member, book.isbn(),
                today, today.plusDays(requestedDays), null);
        LibraryBook checkedOut = new LibraryBook(book.isbn(), book.title(), BookStatus.CHECKED_OUT);
        // Commit: no user callbacks or domain validation after this point.
        activeLoans.put(loanId, loan);
        loanHistory.add(loan);
        checkedOutISBNs.add(book.isbn());
        books.put(book.isbn(), checkedOut);
        usedIds.add(loanId);
        return loan;
    }

    public LoanRecord returnBook(String loanId) {
        String key = Validation.requiredText(loanId, "Loan ID");
        LoanRecord loan = Lookup.require(activeLoans, key, "Active loan");
        LibraryBook book = requireBook(loan.isbn());
        int historyIndex = loanHistory.indexOf(loan);
        if (historyIndex < 0 || book.isAvailable() || !checkedOutISBNs.contains(book.isbn())) {
            throw new IllegalStateException("Inconsistent circulation state for loan " + key);
        }
        LoanRecord returned = new LoanRecord(key, loan.member(), loan.isbn(),
                loan.issueDate(), loan.dueDate(), LocalDate.now(clock));
        LibraryBook available = new LibraryBook(book.isbn(), book.title(), BookStatus.AVAILABLE);
        // Returning releases availability; reservations are processed explicitly.
        loanHistory.set(historyIndex, returned);
        activeLoans.remove(key);
        checkedOutISBNs.remove(book.isbn());
        books.put(book.isbn(), available);
        return returned;
    }

    public Optional<LoanRecord> findActiveLoan(String loanId) {
        return Optional.ofNullable(activeLoans.get(Validation.requiredText(loanId, "Loan ID")));
    }

    public List<LoanRecord> listActiveLoans() { return List.copyOf(activeLoans.values()); }
    public List<LoanRecord> listLoanHistory() { return List.copyOf(loanHistory); }
    public Set<String> getCheckedOutISBNs() { return Set.copyOf(checkedOutISBNs); }

    public Reservation reserveBook(String memberId, String isbn, int requestedDays) {
        Member member = requireMember(memberId);
        LibraryBook book = requireBook(isbn);
        validateDuration(member, requestedDays);
        if (pendingReservations.stream().anyMatch(r -> r.memberId().equals(member.getMemberId())
                && r.isbn().equals(book.isbn()))) {
            throw new DuplicateEntityException("Member already has a pending reservation for ISBN " + book.isbn());
        }
        if (activeLoans.values().stream().anyMatch(l -> l.isbn().equals(book.isbn())
                && l.member().getMemberId().equals(member.getMemberId()))) {
            throw new IllegalArgumentException("Member already holds this book: " + book.isbn());
        }
        if (book.isAvailable() && !hasPendingReservation(book.isbn())) {
            throw new IllegalArgumentException("Book is available; issue a loan instead: " + book.isbn());
        }
        String reservationId = freshId("R-");
        Reservation reservation = new Reservation(reservationId, member.getMemberId(), book.isbn(), requestedDays);
        pendingReservations.add(reservation);
        usedIds.add(reservationId);
        return reservation;
    }

    public Reservation cancelReservation(String reservationId) {
        String key = Validation.requiredText(reservationId, "Reservation ID");
        Iterator<Reservation> iterator = pendingReservations.iterator();
        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            if (reservation.reservationId().equals(key)) {
                iterator.remove();
                return reservation;
            }
        }
        throw new NoSuchElementException("Pending reservation not found: " + key);
    }

    /**
     * Process the earliest fulfillable entry, preserving FIFO for each ISBN.
     * Unavailable entries retain their exact positions; other ISBNs can proceed.
     * If validation/ID creation fails, both queue and circulation stay unchanged.
     */
    public Optional<LoanRecord> processNextReservation() {
        Iterator<Reservation> iterator = pendingReservations.iterator();
        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            Member member = requireMember(reservation.memberId());
            LibraryBook book = requireBook(reservation.isbn());
            validateDuration(member, reservation.requestedDays());
            if (!book.isAvailable()) {
                continue;
            }
            // Queue order guarantees this is the earliest waiter for this book.
            LoanRecord loan = createLoan(member, book, reservation.requestedDays());
            iterator.remove();
            return Optional.of(loan);
        }
        return Optional.empty();
    }

    public List<Reservation> listPendingReservations() { return List.copyOf(pendingReservations); }

    /** Historical view: active overdue first, then due date and stable loan ID. */
    public List<LoanRecord> getSortedLoans() {
        LocalDate today = LocalDate.now(clock);
        Comparator<LoanRecord> order = Comparator
                .comparing((LoanRecord loan) -> !loan.isOverdue(today))
                .thenComparing(LoanRecord::dueDate).thenComparing(LoanRecord::loanId);
        return loanHistory.stream().sorted(order).toList();
    }

    public long getAvailableBookCount() {
        return books.values().stream().filter(LibraryBook::isAvailable).count();
    }

    public int getCheckedOutBookCount() { return checkedOutISBNs.size(); }

    public Map<MembershipType, Long> getActiveLoansByMembershipType() {
        Map<MembershipType, Long> counts = new EnumMap<>(MembershipType.class);
        for (MembershipType type : MembershipType.values()) {
            counts.put(type, 0L);
        }
        for (LoanRecord loan : activeLoans.values()) {
            counts.merge(loan.member().getMembershipType(), 1L, Long::sum);
        }
        return Map.copyOf(counts);
    }

    /** Active-loan estimate only; returned loans retain their final charge in history. */
    public BigDecimal getTotalEstimatedFines() {
        LocalDate today = LocalDate.now(clock);
        return activeLoans.values().stream().map(l -> l.calculateLateCharge(today))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public CirculationSummary getCirculationSummary() {
        return new CirculationSummary(Math.toIntExact(getAvailableBookCount()), getCheckedOutBookCount(),
                activeLoans.size(), loanHistory.size(), pendingReservations.size(),
                getActiveLoansByMembershipType(), getTotalEstimatedFines());
    }

    private boolean hasPendingReservation(String isbn) {
        for (Reservation reservation : pendingReservations) {
            if (reservation.isbn().equals(isbn)) {
                return true;
            }
        }
        return false;
    }

    private Member requireMember(String memberId) {
        return Lookup.require(members, Validation.requiredText(memberId, "Member ID"), "Member");
    }

    private LibraryBook requireBook(String isbn) {
        return Lookup.require(books, Validation.requiredText(isbn, "ISBN"), "Book");
    }

    private String freshId(String prefix) {
        String value = prefix + Validation.requiredText(idSource.get(), "Generated ID");
        if (usedIds.contains(value)) {
            throw new DuplicateEntityException("Generated ID already used: " + value);
        }
        return value;
    }

    private static void validatePositiveDays(int days) {
        if (days < 1) {
            throw new IllegalArgumentException("Requested days must be positive.");
        }
    }

    private static void validateDuration(Member member, int days) {
        validatePositiveDays(days);
        if (days > member.getMaximumLoanDays()) {
            throw new LoanLimitExceededException("Requested " + days + " days; "
                    + member.getMembershipType() + " allows at most " + member.getMaximumLoanDays() + ".");
        }
    }
}
