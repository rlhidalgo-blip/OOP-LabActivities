public class GuestMember extends Member {

    public GuestMember(String memberId, String memberName) {
        super(memberId, memberName);
    }

    @Override
    public int getMaximumLoanDays() {
        return 7;
    }

    @Override
    public double getDailyFine() {
        return 10.00;
    }
}