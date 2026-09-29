public abstract class Member {

    private String memberId;
    private String memberName;

    public Member(String memberId, String memberName) {
        if (memberId == null || memberId.trim().isEmpty()) {
            throw new IllegalArgumentException("Member ID cannot be blank.");
        }

        if (memberName == null || memberName.trim().isEmpty()) {
            throw new IllegalArgumentException("Member name cannot be blank.");
        }

        this.memberId = memberId;
        this.memberName = memberName;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public abstract int getMaximumLoanDays();

    public abstract double getDailyFine();
}