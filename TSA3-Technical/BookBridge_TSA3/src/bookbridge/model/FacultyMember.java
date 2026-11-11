package bookbridge.model;

public final class FacultyMember extends Member {
    public FacultyMember(String memberId, String memberName) {
        super(memberId, memberName);
    }

    @Override public int getMaximumLoanDays() { return 30; }
    @Override public double getDailyFine() { return 2.00; }
    @Override public MembershipType getMembershipType() { return MembershipType.FACULTY; }
}
