public class PolymorphismTest {

    public static void main(String[] args) {

        Member member1 = new StudentMember("S100", "Test Student");
        Member member2 = new FacultyMember("F100", "Test Faculty");
        Member member3 = new GuestMember("G100", "Test Guest");

        System.out.println("=== POLYMORPHISM TEST 1 ===");

        System.out.println(
                "Student Maximum Loan Days: "
                        + member1.getMaximumLoanDays());

        System.out.println(
                "Faculty Maximum Loan Days: "
                        + member2.getMaximumLoanDays());

        System.out.println(
                "Guest Maximum Loan Days: "
                        + member3.getMaximumLoanDays());

        System.out.println();

        System.out.println("=== POLYMORPHISM TEST 2 ===");

        System.out.printf(
                "Student Daily Fine: PHP %.2f%n",
                member1.getDailyFine());

        System.out.printf(
                "Faculty Daily Fine: PHP %.2f%n",
                member2.getDailyFine());

        System.out.printf(
                "Guest Daily Fine: PHP %.2f%n",
                member3.getDailyFine());
    }
}