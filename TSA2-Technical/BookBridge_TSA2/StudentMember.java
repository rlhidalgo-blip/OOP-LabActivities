public class StudentMember extends Member {

    public StudentMember(String memberId, String memberName) {
        super(memberId, memberName);
    }

    @Override
    public int getMaximumLoanDays() {
        return 14;
    }

    @Override
    public double getDailyFine() {
        return 5.00;
    }
}