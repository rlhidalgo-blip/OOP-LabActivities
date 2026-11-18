/** Runs against untouched default-package TSA2 classes, not TSA3 replacements. */
public final class LegacyRegressionTest {
    private LegacyRegressionTest() { }

    public static void main(String[] args) {
        Member[] members = {new StudentMember("S", "Student"),
                new FacultyMember("F", "Faculty"), new GuestMember("G", "Guest")};
        int[] days = {14, 30, 7};
        double[] rates = {5, 2, 10};
        LibraryService service = new LibraryService();
        for (int i = 0; i < members.length; i++) {
            LibraryBook available = new LibraryBook("A", "Available", true);
            LibraryBook unavailable = new LibraryBook("B", "Unavailable", false);
            check(members[i].getMaximumLoanDays() == days[i], "Maximum days");
            check(members[i].getDailyFine() == rates[i], "Daily rate");
            check(service.classifyRequest(members[i], available, days[i]).equals("APPROVED"), "Boundary");
            check(service.classifyRequest(members[i], available, days[i] + 1).equals("DENIED"), "Denial");
            check(service.classifyRequest(members[i], unavailable, days[i]).equals("RESERVED"), "Reservation");
            LoanRecord loan = new LoanRecord(members[i], available, days[i], 3);
            check(loan.calculateLateCharge() == rates[i] * 3, "Fine calculation");
            available.markBorrowed();
            loan.markReturned();
            check(loan.isReturned() && available.isAvailable(), "Original return behavior");
            System.out.println("PASS LEGACY-" + (i + 1) + " " + members[i].getClass().getSimpleName());
        }
        check(service.classifyRequest(members[0], new LibraryBook("C", "Unavailable", false), 15)
                .equals("RESERVED"), "Document original availability-before-duration behavior");
        System.out.println("PASS LEGACY-4 original over-limit/unavailable precedence captured");
        System.out.println("TSA2 assertion regression: 4 passed, 0 failed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
