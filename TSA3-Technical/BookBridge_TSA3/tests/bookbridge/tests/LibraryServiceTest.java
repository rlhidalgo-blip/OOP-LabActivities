package bookbridge.tests;

import bookbridge.exception.DuplicateEntityException;
import bookbridge.exception.ItemUnavailableException;
import bookbridge.exception.LoanLimitExceededException;
import bookbridge.model.BookStatus;
import bookbridge.model.FacultyMember;
import bookbridge.model.GuestMember;
import bookbridge.model.LibraryBook;
import bookbridge.model.LoanRecord;
import bookbridge.model.LoanStatus;
import bookbridge.model.MembershipType;
import bookbridge.model.RequestOutcome;
import bookbridge.model.StudentMember;
import bookbridge.service.LibraryService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Dependency-free assertions always run, even without the JVM -ea option. */
public final class LibraryServiceTest {
    private static int passed;
    private static int failed;

    private LibraryServiceTest() { }

    public static void main(String[] args) {
        test("T01 registration and book addition", LibraryServiceTest::testRegistrationAndBookAddition);
        test("T02 duplicate member and ISBN rejection", LibraryServiceTest::testDuplicateMemberAndISBNRejection);
        test("T03 successful issuance and due date", LibraryServiceTest::testLoanIssuanceAndDueDate);
        test("T04 unavailable book rejection", LibraryServiceTest::testUnavailableBookRejection);
        test("T05 membership duration violation", LibraryServiceTest::testMembershipDurationLimits);
        test("T06 successful and duplicate return with final fine", LibraryServiceTest::testReturnAndFinalFine);
        test("T07 empty and wholly unavailable queues", LibraryServiceTest::testEmptyAndUnavailableQueues);
        test("T08 missing searches and operation entities", LibraryServiceTest::testMissingEntitySearchesAndOperations);
        test("T09 overdue due-date and ID sorting ties", LibraryServiceTest::testLoanSortingWithTies);
        test("T10 aggregate counts groups and fine totals", LibraryServiceTest::testAggregateCountsAndFines);
        test("T11 per-book FIFO and independent book progress", LibraryServiceTest::testReservationFIFOAndIndependentBookProgress);
        test("T12 failed issue leaves every collection unchanged", LibraryServiceTest::testFailedIssuePreservesCollections);
        test("T13 immutable collection and domain snapshots", LibraryServiceTest::testImmutableSnapshots);
        test("T14 title searches and status membership filters", LibraryServiceTest::testTitleSearchAndFilters);
        test("T15 reservation cancellation preserves remaining order", LibraryServiceTest::testReservationCancellationOrder);
        test("T16 duplicate and invalid reservations", LibraryServiceTest::testInvalidAndDuplicateReservations);
        test("T17 classification denial precedence and no mutation", LibraryServiceTest::testClassificationPrecedence);
        test("T18 duplicate generated ID leaves queue and books intact", LibraryServiceTest::testProcessingIDConflictPreservesQueue);
        test("T19 return with backward clock is atomic", LibraryServiceTest::testBackwardClockReturnIsAtomic);
        test("T20 invalid inputs and imported checkout rejection", LibraryServiceTest::testInvalidInputsAndImportedCheckout);
        test("T21 no invented simultaneous loan count limit", LibraryServiceTest::testNoSimultaneousLoanCountCap);
        test("T22 supplier failure is propagated without state change", LibraryServiceTest::testIDSupplierFailurePreservesState);
        test("T23 cancelled reservation IDs cannot be reused", LibraryServiceTest::testCancelledIDCannotBeReused);
        test("T24 empty reports include zero membership groups", LibraryServiceTest::testEmptyReports);
        test("T25 stale return cannot release a reissued book", LibraryServiceTest::testStaleReturnAfterReissue);
        test("T26 available reserved book retains FIFO after cancellation", LibraryServiceTest::testAvailableQueuedBookAfterCancellation);
        System.out.println("TSA3 service tests: " + passed + " passed, " + failed + " failed");
        if (failed != 0) { throw new AssertionError("TSA3 test failures: " + failed); }
    }

    private static void testRegistrationAndBookAddition() {
        var s = fixture(new MutableClock());
        eq(s.listMembers().size(), 3);
        eq(s.listBooks().size(), 4);
        eq(s.findMember(" S ").orElseThrow().getMemberName(), "Student");
        eq(s.findBookByISBN(" A ").orElseThrow().title(), "Java Collections");
        consistent(s);
    }

    private static void testDuplicateMemberAndISBNRejection() {
        var s = fixture(new MutableClock());
        var before = s.getCirculationSummary();
        expect(DuplicateEntityException.class, () -> s.registerMember(new GuestMember(" S ", "Other")));
        expect(DuplicateEntityException.class, () -> s.addBook(new LibraryBook(" A ", "Other")));
        eq(s.listMembers().size(), 3);
        eq(s.getCirculationSummary(), before);
        consistent(s);
    }

    private static void testLoanIssuanceAndDueDate() {
        var s = fixture(new MutableClock());
        var snapshot = s.findBookByISBN("A").orElseThrow();
        var loan = s.issueLoan("S", "A", 14);
        eq(loan.issueDate(), LocalDate.of(2026, 11, 1));
        eq(loan.dueDate(), LocalDate.of(2026, 11, 15));
        eq(loan.status(), LoanStatus.ACTIVE);
        eq(s.findActiveLoan(loan.loanId()).orElseThrow(), loan);
        check(snapshot.isAvailable(), "Previously returned snapshot must remain immutable");
        eq(s.getCheckedOutBookCount(), 1);
        consistent(s);
    }

    private static void testUnavailableBookRejection() {
        var s = fixture(new MutableClock());
        s.issueLoan("S", "A", 1);
        var before = s.getCirculationSummary();
        expect(ItemUnavailableException.class, () -> s.issueLoan("F", "A", 2));
        eq(s.getCirculationSummary(), before);
        consistent(s);
    }

    private static void testMembershipDurationLimits() {
        var s = fixture(new MutableClock());
        expect(LoanLimitExceededException.class, () -> s.issueLoan("S", "A", 15));
        expect(LoanLimitExceededException.class, () -> s.issueLoan("F", "A", 31));
        expect(LoanLimitExceededException.class, () -> s.issueLoan("G", "A", 8));
        eq(s.listLoanHistory().size(), 0);
        eq(s.getAvailableBookCount(), 4L);
        consistent(s);
    }

    private static void testReturnAndFinalFine() {
        var clock = new MutableClock();
        var s = fixture(clock);
        var loan = s.issueLoan("S", "A", 1);
        clock.day(5);
        var returned = s.returnBook(loan.loanId());
        eq(returned.status(), LoanStatus.RETURNED);
        money(returned.calculateLateCharge(LocalDate.of(2030, 1, 1)), "15");
        check(s.findActiveLoan(loan.loanId()).isEmpty(), "Returned loan remains active");
        check(s.findBookByISBN("A").orElseThrow().isAvailable(), "Book not released");
        eq(s.listLoanHistory(), List.of(returned));
        expect(NoSuchElementException.class, () -> s.returnBook(loan.loanId()));
        consistent(s);
    }

    private static void testEmptyAndUnavailableQueues() {
        var s = fixture(new MutableClock());
        check(s.processNextReservation().isEmpty(), "Empty queue result");
        s.issueLoan("S", "A", 1);
        s.reserveBook("F", "A", 2);
        var before = s.listPendingReservations();
        check(s.processNextReservation().isEmpty(), "Unavailable queue result");
        eq(s.listPendingReservations(), before);
        consistent(s);
    }

    private static void testMissingEntitySearchesAndOperations() {
        var s = fixture(new MutableClock());
        check(s.findMember("missing").isEmpty(), "Missing member lookup");
        check(s.findBookByISBN("missing").isEmpty(), "Missing ISBN lookup");
        check(s.findActiveLoan("missing").isEmpty(), "Missing loan lookup");
        expect(NoSuchElementException.class, () -> s.issueLoan("missing", "A", 1));
        expect(NoSuchElementException.class, () -> s.issueLoan("S", "missing", 1));
        expect(NoSuchElementException.class, () -> s.returnBook("missing"));
        expect(NoSuchElementException.class, () -> s.reserveBook("missing", "A", 1));
        expect(NoSuchElementException.class, () -> s.reserveBook("S", "missing", 1));
        consistent(s);
    }

    private static void testLoanSortingWithTies() {
        var clock = new MutableClock();
        List<String> ids = List.of("30", "20", "10", "40");
        var index = new AtomicInteger();
        var s = new LibraryService(clock, () -> ids.get(index.getAndIncrement()));
        populate(s);
        var returned = s.issueLoan("S", "D", 1);
        s.returnBook(returned.loanId());
        var laterId = s.issueLoan("S", "A", 2);
        var earlierId = s.issueLoan("S", "B", 2);
        var dueToday = s.issueLoan("S", "C", 4);
        clock.day(5);
        var original = s.listLoanHistory();
        eq(s.getSortedLoans(), List.of(earlierId, laterId, s.listLoanHistory().get(0), dueToday));
        check(!dueToday.isOverdue(LocalDate.of(2026, 11, 5)), "Due today is not overdue");
        eq(s.listLoanHistory(), original);
        consistent(s);
    }

    private static void testAggregateCountsAndFines() {
        var clock = new MutableClock();
        var s = fixture(clock);
        var student = s.issueLoan("S", "A", 1);
        s.issueLoan("F", "B", 2);
        s.issueLoan("G", "C", 3);
        clock.day(5);
        var report = s.getCirculationSummary();
        eq(report.availableBooks(), 1);
        eq(report.checkedOutBooks(), 3);
        eq(report.activeLoans(), 3);
        eq(report.historicalLoans(), 3);
        eq(report.pendingReservations(), 0);
        for (MembershipType type : MembershipType.values()) {
            eq(report.activeLoansByMembershipType().get(type), 1L);
        }
        money(report.estimatedFines(), "29"); // 3*5 + 2*2 + 1*10
        s.returnBook(student.loanId());
        money(s.getTotalEstimatedFines(), "14");
        eq(s.getActiveLoansByMembershipType().get(MembershipType.STUDENT), 0L);
        consistent(s);
    }

    private static void testReservationFIFOAndIndependentBookProgress() {
        var s = fixture(new MutableClock());
        var a = s.issueLoan("S", "A", 1);
        var b = s.issueLoan("S", "B", 1);
        var firstA = s.reserveBook("F", "A", 2);
        var secondA = s.reserveBook("G", "A", 3);
        var firstB = s.reserveBook("G", "B", 2);
        s.returnBook(b.loanId());
        var processedB = s.processNextReservation().orElseThrow();
        eq(processedB.isbn(), "B");
        eq(processedB.member().getMemberId(), firstB.memberId());
        eq(s.listPendingReservations(), List.of(firstA, secondA));
        s.returnBook(a.loanId());
        expect(ItemUnavailableException.class, () -> s.issueLoan("G", "A", 1));
        expect(ItemUnavailableException.class, () -> s.issueLoan("F", "A", 1));
        var processedA = s.processNextReservation().orElseThrow();
        eq(processedA.member().getMemberId(), "F");
        eq(s.listPendingReservations(), List.of(secondA));
        s.returnBook(processedA.loanId());
        eq(s.processNextReservation().orElseThrow().member().getMemberId(), "G");
        eq(s.listPendingReservations().size(), 0);
        consistent(s);
    }

    private static void testFailedIssuePreservesCollections() {
        var s = fixture(new MutableClock());
        var books = s.listBooks();
        var members = s.listMembers();
        var history = s.listLoanHistory();
        var active = s.listActiveLoans();
        var queue = s.listPendingReservations();
        var checked = s.getCheckedOutISBNs();
        expect(IllegalArgumentException.class, () -> s.issueLoan("S", "A", 0));
        expect(LoanLimitExceededException.class, () -> s.issueLoan("G", "A", 8));
        eq(s.listBooks(), books);
        eq(s.listMembers(), members);
        eq(s.listLoanHistory(), history);
        eq(s.listActiveLoans(), active);
        eq(s.listPendingReservations(), queue);
        eq(s.getCheckedOutISBNs(), checked);
        consistent(s);
    }

    private static void testImmutableSnapshots() {
        var s = fixture(new MutableClock());
        var oldBooks = s.listBooks();
        var loan = s.issueLoan("S", "A", 1);
        s.reserveBook("F", "A", 1);
        expect(UnsupportedOperationException.class, () -> s.listBooks().clear());
        expect(UnsupportedOperationException.class, () -> s.listMembers().clear());
        expect(UnsupportedOperationException.class, () -> s.listActiveLoans().clear());
        expect(UnsupportedOperationException.class, () -> s.listLoanHistory().clear());
        expect(UnsupportedOperationException.class, () -> s.listPendingReservations().clear());
        expect(UnsupportedOperationException.class, () -> s.getCheckedOutISBNs().clear());
        expect(UnsupportedOperationException.class, () -> s.getSortedLoans().clear());
        expect(UnsupportedOperationException.class, () -> s.getCirculationSummary()
                .activeLoansByMembershipType().clear());
        s.returnBook(loan.loanId());
        eq(loan.status(), LoanStatus.ACTIVE);
        check(oldBooks.get(0).isAvailable(), "Old book snapshot changed");
        consistent(s);
    }

    private static void testTitleSearchAndFilters() {
        var s = fixture(new MutableClock());
        eq(s.searchBooksByTitle(" jAvA ").size(), 2);
        eq(s.searchBooksByTitle("not present").size(), 0);
        s.issueLoan("S", "A", 1);
        eq(s.filterBooksByStatus(BookStatus.CHECKED_OUT).size(), 1);
        eq(s.filterBooksByStatus(BookStatus.AVAILABLE).size(), 3);
        eq(s.filterMembersByMembershipType(MembershipType.GUEST).get(0).getMemberId(), "G");
        expect(UnsupportedOperationException.class, () -> s.searchBooksByTitle("java").clear());
        expect(UnsupportedOperationException.class, () -> s.filterBooksByStatus(BookStatus.AVAILABLE).clear());
        expect(UnsupportedOperationException.class, () -> s.filterMembersByMembershipType(MembershipType.GUEST).clear());
        consistent(s);
    }

    private static void testReservationCancellationOrder() {
        var s = fixture(new MutableClock());
        s.issueLoan("S", "A", 1);
        s.issueLoan("S", "B", 1);
        var first = s.reserveBook("F", "A", 1);
        var middle = s.reserveBook("G", "B", 1);
        var last = s.reserveBook("G", "A", 1);
        eq(s.cancelReservation(middle.reservationId()), middle);
        eq(s.listPendingReservations(), List.of(first, last));
        expect(NoSuchElementException.class, () -> s.cancelReservation(middle.reservationId()));
        eq(s.listPendingReservations(), List.of(first, last));
        consistent(s);
    }

    private static void testInvalidAndDuplicateReservations() {
        var s = fixture(new MutableClock());
        expect(IllegalArgumentException.class, () -> s.reserveBook("F", "A", 1));
        s.issueLoan("S", "A", 1);
        expect(IllegalArgumentException.class, () -> s.reserveBook("S", "A", 1));
        s.reserveBook("G", "A", 7);
        expect(DuplicateEntityException.class, () -> s.reserveBook("G", "A", 7));
        expect(LoanLimitExceededException.class, () -> s.reserveBook("G", "A", 8));
        expect(IllegalArgumentException.class, () -> s.reserveBook("F", "A", -1));
        eq(s.listPendingReservations().size(), 1);
        consistent(s);
    }

    private static void testClassificationPrecedence() {
        var s = fixture(new MutableClock());
        eq(s.classifyRequest("S", "A", 14), RequestOutcome.APPROVED);
        s.issueLoan("F", "A", 30);
        var before = s.getCirculationSummary();
        eq(s.classifyRequest("S", "A", 14), RequestOutcome.RESERVED);
        eq(s.classifyRequest("S", "A", 15), RequestOutcome.DENIED);
        expect(IllegalArgumentException.class, () -> s.classifyRequest("S", "A", 0));
        eq(s.getCirculationSummary(), before);
        consistent(s);
    }

    private static void testProcessingIDConflictPreservesQueue() {
        var clock = new MutableClock();
        var s = new LibraryService(clock, () -> "same");
        populate(s);
        var loan = s.issueLoan("S", "A", 1);
        s.reserveBook("F", "A", 1); // R-same and L-same are separate namespaces
        s.returnBook(loan.loanId());
        var before = s.getCirculationSummary();
        var queue = s.listPendingReservations();
        expect(DuplicateEntityException.class, s::processNextReservation);
        eq(s.getCirculationSummary(), before);
        eq(s.listPendingReservations(), queue);
        consistent(s);
    }

    private static void testBackwardClockReturnIsAtomic() {
        var clock = new MutableClock();
        var s = fixture(clock);
        var loan = s.issueLoan("S", "A", 1);
        clock.instant = Instant.parse("2026-10-31T00:00:00Z");
        var before = s.listLoanHistory();
        expect(IllegalArgumentException.class, () -> s.returnBook(loan.loanId()));
        eq(s.listLoanHistory(), before);
        eq(s.findActiveLoan(loan.loanId()).orElseThrow(), loan);
        consistent(s);
    }

    private static void testInvalidInputsAndImportedCheckout() {
        var s = fixture(new MutableClock());
        expect(IllegalArgumentException.class, () -> new StudentMember(" ", "Name"));
        expect(IllegalArgumentException.class, () -> new LibraryBook("A", " "));
        expect(IllegalArgumentException.class, () -> s.findMember(null));
        expect(IllegalArgumentException.class, () -> s.findBookByISBN(" "));
        expect(IllegalArgumentException.class, () -> s.searchBooksByTitle(" "));
        expect(IllegalArgumentException.class, () -> s.findActiveLoan(" "));
        expect(IllegalArgumentException.class, () -> s.cancelReservation(null));
        expect(NullPointerException.class, () -> s.registerMember(null));
        expect(NullPointerException.class, () -> s.addBook(null));
        expect(NullPointerException.class, () -> s.filterBooksByStatus(null));
        expect(NullPointerException.class, () -> s.filterMembersByMembershipType(null));
        expect(IllegalArgumentException.class,
                () -> s.addBook(new LibraryBook("X", "Imported", BookStatus.CHECKED_OUT)));
        eq(s.listBooks().size(), 4);
        consistent(s);
    }

    private static void testNoSimultaneousLoanCountCap() {
        var s = fixture(new MutableClock());
        for (var book : s.listBooks()) { s.issueLoan("G", book.isbn(), 7); }
        eq(s.listActiveLoans().size(), 4);
        consistent(s);
    }

    private static void testIDSupplierFailurePreservesState() {
        var s = new LibraryService(new MutableClock(), () -> {
            throw new IllegalStateException("Test ID source failure");
        });
        populate(s);
        var before = s.getCirculationSummary();
        expect(IllegalStateException.class, () -> s.issueLoan("S", "A", 1));
        eq(s.getCirculationSummary(), before);
        consistent(s);
    }

    private static void testCancelledIDCannotBeReused() {
        var s = new LibraryService(new MutableClock(), () -> "same");
        populate(s);
        s.issueLoan("S", "A", 1);
        var reservation = s.reserveBook("F", "A", 1);
        s.cancelReservation(reservation.reservationId());
        expect(DuplicateEntityException.class, () -> s.reserveBook("G", "A", 1));
        eq(s.listPendingReservations().size(), 0);
        consistent(s);
    }

    private static void testEmptyReports() {
        var s = new LibraryService(new MutableClock());
        eq(s.getAvailableBookCount(), 0L);
        eq(s.getCheckedOutBookCount(), 0);
        eq(s.getSortedLoans().size(), 0);
        money(s.getTotalEstimatedFines(), "0");
        eq(s.getActiveLoansByMembershipType().size(), 3);
        for (long value : s.getActiveLoansByMembershipType().values()) { eq(value, 0L); }
        consistent(s);
    }

    private static void testStaleReturnAfterReissue() {
        var s = fixture(new MutableClock());
        var oldLoan = s.issueLoan("S", "A", 1);
        s.returnBook(oldLoan.loanId());
        var newLoan = s.issueLoan("G", "A", 7);
        expect(NoSuchElementException.class, () -> s.returnBook(oldLoan.loanId()));
        eq(s.findActiveLoan(newLoan.loanId()).orElseThrow(), newLoan);
        eq(s.listLoanHistory().size(), 2);
        consistent(s);
    }

    private static void testAvailableQueuedBookAfterCancellation() {
        var s = fixture(new MutableClock());
        var loan = s.issueLoan("S", "A", 1);
        var first = s.reserveBook("F", "A", 2);
        s.returnBook(loan.loanId());
        var second = s.reserveBook("G", "A", 3);
        eq(s.listPendingReservations(), List.of(first, second));
        eq(s.classifyRequest("S", "A", 1), RequestOutcome.RESERVED);
        s.cancelReservation(first.reservationId());
        eq(s.processNextReservation().orElseThrow().member().getMemberId(), "G");
        check(s.processNextReservation().isEmpty(), "Queue should now be empty");
        consistent(s);
    }

    private static LibraryService fixture(MutableClock clock) {
        var ids = new AtomicInteger();
        var service = new LibraryService(clock, () -> String.format("%03d", ids.incrementAndGet()));
        populate(service);
        return service;
    }

    private static void populate(LibraryService s) {
        s.registerMember(new StudentMember("S", "Student"));
        s.registerMember(new FacultyMember("F", "Faculty"));
        s.registerMember(new GuestMember("G", "Guest"));
        s.addBook(new LibraryBook("A", "Java Collections"));
        s.addBook(new LibraryBook("B", "Java Design"));
        s.addBook(new LibraryBook("C", "Algorithms"));
        s.addBook(new LibraryBook("D", "Library Systems"));
    }

    /** Verify all redundant circulation representations after each scenario. */
    private static void consistent(LibraryService s) {
        Set<String> activeISBNs = new HashSet<>();
        Set<String> loanIds = new HashSet<>();
        for (var loan : s.listLoanHistory()) {
            check(loanIds.add(loan.loanId()), "Duplicate historical loan ID");
            if (loan.status() == LoanStatus.ACTIVE) {
                eq(s.findActiveLoan(loan.loanId()).orElseThrow(), loan);
            } else {
                check(s.findActiveLoan(loan.loanId()).isEmpty(), "Returned loan in active map");
            }
        }
        for (var loan : s.listActiveLoans()) {
            check(activeISBNs.add(loan.isbn()), "Multiple active loans for one ISBN");
            eq(s.findBookByISBN(loan.isbn()).orElseThrow().status(), BookStatus.CHECKED_OUT);
            check(s.listLoanHistory().contains(loan), "Active loan absent from history");
        }
        eq(activeISBNs, s.getCheckedOutISBNs());
        eq(activeISBNs.size(), s.getCheckedOutBookCount());
        for (var book : s.listBooks()) {
            eq(book.status() == BookStatus.CHECKED_OUT, activeISBNs.contains(book.isbn()));
        }
        eq(s.getAvailableBookCount() + s.getCheckedOutBookCount(), (long) s.listBooks().size());
        Set<String> reservationIds = new HashSet<>();
        Set<String> pairs = new HashSet<>();
        for (var reservation : s.listPendingReservations()) {
            check(reservationIds.add(reservation.reservationId()), "Duplicate reservation ID");
            check(pairs.add(reservation.memberId() + ":" + reservation.isbn()), "Duplicate reservation pair");
            check(s.findMember(reservation.memberId()).isPresent(), "Unknown reservation member");
            check(s.findBookByISBN(reservation.isbn()).isPresent(), "Unknown reservation book");
        }
    }

    private static void test(String name, Runnable scenario) {
        try {
            scenario.run();
            passed++;
            System.out.println("PASS " + name);
        } catch (RuntimeException | AssertionError error) {
            failed++;
            System.out.println("FAIL " + name + ": " + error);
            error.printStackTrace(System.out);
        }
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException actual) {
            if (type.isInstance(actual)) { return; }
            throw new AssertionError("Expected " + type.getSimpleName() + ", got " + actual, actual);
        }
        throw new AssertionError("Expected " + type.getSimpleName() + " but operation succeeded");
    }

    private static void money(BigDecimal actual, String expected) {
        check(actual.compareTo(new BigDecimal(expected)) == 0, "Expected PHP " + expected + ", got " + actual);
    }

    private static void eq(Object actual, Object expected) {
        check(java.util.Objects.equals(actual, expected), "Expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-11-01T00:00:00Z");
        void day(int day) { instant = LocalDate.of(2026, 11, day).atStartOfDay(ZoneOffset.UTC).toInstant(); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant, zone); }
        @Override public Instant instant() { return instant; }
    }
}
