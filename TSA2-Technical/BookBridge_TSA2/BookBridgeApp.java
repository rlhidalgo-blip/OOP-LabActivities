import java.util.Scanner;

public class BookBridgeApp {

    static final int TOTAL_REQUESTS = 5;

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        Member[] members = new Member[TOTAL_REQUESTS];
        LibraryBook[] books = new LibraryBook[TOTAL_REQUESTS];
        LoanRecord[] loans = new LoanRecord[TOTAL_REQUESTS];
        String[] outcomes = new String[TOTAL_REQUESTS];

        LibraryService service = new LibraryService();

        System.out.println("========================================");
        System.out.println("       BOOKBRIDGE LIBRARY SYSTEM");
        System.out.println("========================================");

        for (int i = 0; i < TOTAL_REQUESTS; i++) {

            System.out.println();
            System.out.println("Transaction " + (i + 1));
            System.out.println("----------------------------------------");

            String memberId =
                    readRequiredText(scanner, "Member ID: ");

            String memberName =
                    readRequiredText(scanner, "Member Name: ");

            String membershipType =
                    readMembershipType(scanner);

            members[i] = createMember(
                    membershipType,
                    memberId,
                    memberName);

            String isbn =
                    readRequiredText(scanner, "ISBN: ");

            String title =
                    readRequiredText(scanner, "Book Title: ");

            int requestedDays = readRequestedDays(scanner);
            boolean available = readAvailability(scanner);
            int lateDays = readLateDays(scanner);

            books[i] = new LibraryBook(
                    isbn,
                    title,
                    available);

            outcomes[i] = service.classifyRequest(
                    members[i],
                    books[i],
                    requestedDays);

            loans[i] = new LoanRecord(
                    members[i],
                    books[i],
                    requestedDays,
                    lateDays);

            if (outcomes[i].equals("APPROVED")) {
                books[i].markBorrowed();
            }

            if (outcomes[i].equals("RESERVED")) {
                Reservation reservation =
                        new Reservation(members[i], books[i]);

                reservation.sendNotification();
            }

            printTransactionResult(
                    members[i],
                    books[i],
                    loans[i],
                    membershipType,
                    outcomes[i]);
        }

        printSummary(members, books, loans, outcomes);

        scanner.close();
    }

    public static Member createMember(
            String membershipType,
            String memberId,
            String memberName) {

        if (membershipType.equals("STUDENT")) {
            return new StudentMember(memberId, memberName);
        }

        if (membershipType.equals("FACULTY")) {
            return new FacultyMember(memberId, memberName);
        }

        return new GuestMember(memberId, memberName);
    }

    public static boolean validateMembershipType(
            String membershipType) {

        return membershipType.equals("STUDENT")
                || membershipType.equals("FACULTY")
                || membershipType.equals("GUEST");
    }

    public static String readRequiredText(
            Scanner scanner,
            String prompt) {

        while (true) {

            System.out.print(prompt);
            String value = scanner.nextLine().trim();

            if (!value.isEmpty()) {
                return value;
            }

            System.out.println(
                    "Invalid input. This field cannot be blank.");
        }
    }

    public static String readMembershipType(
            Scanner scanner) {

        while (true) {

            System.out.print(
                    "Membership Type (STUDENT/FACULTY/GUEST): ");

            String membershipType =
                    scanner.nextLine().trim().toUpperCase();

            if (validateMembershipType(membershipType)) {
                return membershipType;
            }

            System.out.println(
                    "Invalid membership type. Please try again.");
        }
    }

    public static int readRequestedDays(
            Scanner scanner) {

        while (true) {

            System.out.print(
                    "Requested Loan Days (1-30): ");

            String input = scanner.nextLine().trim();

            try {

                int days = Integer.parseInt(input);

                if (days >= 1 && days <= 30) {
                    return days;
                }

                System.out.println(
                        "Invalid input. Requested days must be from 1 to 30.");

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid input. Please enter a whole number.");
            }
        }
    }

    public static boolean readAvailability(
            Scanner scanner) {

        while (true) {

            System.out.print(
                    "Is the book available? (YES/NO): ");

            String input =
                    scanner.nextLine().trim().toUpperCase();

            if (input.equals("YES")) {
                return true;
            }

            if (input.equals("NO")) {
                return false;
            }

            System.out.println(
                    "Invalid input. Please enter YES or NO.");
        }
    }

    public static int readLateDays(
            Scanner scanner) {

        while (true) {

            System.out.print("Late Days: ");

            String input = scanner.nextLine().trim();

            try {

                int days = Integer.parseInt(input);

                if (days >= 0) {
                    return days;
                }

                System.out.println(
                        "Invalid input. Late days cannot be negative.");

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid input. Please enter a whole number.");
            }
        }
    }

    public static void printTransactionResult(
            Member member,
            LibraryBook book,
            LoanRecord loan,
            String membershipType,
            String outcome) {

        System.out.println();
        System.out.println(
                "---------- TRANSACTION RESULT ----------");

        System.out.println(
                "Member ID       : " + member.getMemberId());

        System.out.println(
                "Member Name     : " + member.getMemberName());

        System.out.println(
                "Membership Type : " + membershipType);

        System.out.println(
                "ISBN            : " + book.getIsbn());

        System.out.println(
                "Book Title      : " + book.getTitle());

        System.out.println(
                "Requested Days  : " + loan.getRequestedDays());

        System.out.println(
                "Late Days       : " + loan.getLateDays());

        System.out.println(
                "Outcome         : " + outcome);

        System.out.printf(
                "Estimated Late Charge: PHP %.2f%n",
                loan.calculateLateCharge());

        System.out.println("----------------------------------------");
    }

    public static void printSummary(
            Member[] members,
            LibraryBook[] books,
            LoanRecord[] loans,
            String[] outcomes) {

        int approvedCount = 0;
        int reservedCount = 0;
        int deniedCount = 0;

        double totalLateCharge = 0.0;

        int greatestRequestIndex = 0;

        for (int i = 0; i < outcomes.length; i++) {

            if (outcomes[i].equals("APPROVED")) {
                approvedCount++;
            } else if (outcomes[i].equals("RESERVED")) {
                reservedCount++;
            } else if (outcomes[i].equals("DENIED")) {
                deniedCount++;
            }

            totalLateCharge +=
                    loans[i].calculateLateCharge();

            if (loans[i].getRequestedDays()
                    > loans[greatestRequestIndex]
                    .getRequestedDays()) {

                greatestRequestIndex = i;
            }
        }

        System.out.println();
        System.out.println("========================================");
        System.out.println("          BOOKBRIDGE SUMMARY");
        System.out.println("========================================");

        System.out.println(
                "Approved Requests : " + approvedCount);

        System.out.println(
                "Reserved Requests : " + reservedCount);

        System.out.println(
                "Denied Requests   : " + deniedCount);

        System.out.println();
        System.out.println(
                "Request With Greatest Requested Days");

        System.out.println(
                "Member ID      : "
                        + members[greatestRequestIndex]
                        .getMemberId());

        System.out.println(
                "Member Name    : "
                        + members[greatestRequestIndex]
                        .getMemberName());

        System.out.println(
                "Book Title     : "
                        + books[greatestRequestIndex]
                        .getTitle());

        System.out.println(
                "Requested Days : "
                        + loans[greatestRequestIndex]
                        .getRequestedDays());

        System.out.printf(
                "%nTotal Estimated Late Charge: PHP %.2f%n",
                totalLateCharge);

        System.out.println("========================================");
    }
}