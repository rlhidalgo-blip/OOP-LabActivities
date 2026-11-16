package bookbridge.model;

import bookbridge.util.Validation;

/** TSA2 abstraction retained, with final identity fields and polymorphic policies. */
public abstract class Member {
    private final String memberId;
    private final String memberName;

    protected Member(String memberId, String memberName) {
        this.memberId = Validation.requiredText(memberId, "Member ID");
        this.memberName = Validation.requiredText(memberName, "Member name");
    }

    public final String getMemberId() { return memberId; }
    public final String getMemberName() { return memberName; }
    @Override public final String toString() {
        return getMembershipType() + " " + memberId + " (" + memberName + ")";
    }
    public abstract int getMaximumLoanDays();
    public abstract double getDailyFine();
    public abstract MembershipType getMembershipType();
}
