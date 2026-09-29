public class LoanRecord {

    private static int nextLoanNumber = 1;

    private int loanNumber;
    private Member member;
    private LibraryBook book;
    private int requestedDays;
    private int lateDays;
    private boolean returned;

    public LoanRecord(Member member, LibraryBook book,
                      int requestedDays, int lateDays) {

        if (requestedDays < 1 || requestedDays > 30) {
            throw new IllegalArgumentException(
                    "Requested days must be from 1 to 30.");
        }

        if (lateDays < 0) {
            throw new IllegalArgumentException(
                    "Late days cannot be negative.");
        }

        this.loanNumber = nextLoanNumber;
        nextLoanNumber++;

        this.member = member;
        this.book = book;
        this.requestedDays = requestedDays;
        this.lateDays = lateDays;
        this.returned = false;
    }

    public int getLoanNumber() {
        return loanNumber;
    }

    public Member getMember() {
        return member;
    }

    public LibraryBook getBook() {
        return book;
    }

    public int getRequestedDays() {
        return requestedDays;
    }

    public int getLateDays() {
        return lateDays;
    }

    public double calculateLateCharge() {
        return lateDays * member.getDailyFine();
    }

    public void markReturned() {
        returned = true;
        book.markReturned();
    }

    public boolean isReturned() {
        return returned;
    }
}