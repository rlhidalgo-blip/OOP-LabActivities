import java.util.Scanner;

public class BookBridgeApp {

    // Named constants
    static final int TOTAL_REQUESTS = 5;

    static final int STUDENT_MAX_DAYS = 14;
    static final int FACULTY_MAX_DAYS = 30;
    static final int GUEST_MAX_DAYS = 7;

    static final double STUDENT_FINE = 5.00;
    static final double FACULTY_FINE = 2.00;
    static final double GUEST_FINE = 10.00;

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        // Coordinated arrays for exactly five valid requests
        String[] memberIds = new String[TOTAL_REQUESTS];
        String[] memberNames = new String[TOTAL_REQUESTS];
        String[] membershipTypes = new String[TOTAL_REQUESTS];
        String[] isbns = new String[TOTAL_REQUESTS];
        String[] bookTitles = new String[TOTAL_REQUESTS];

        int[] requestedDays = new int[TOTAL_REQUESTS];
        boolean[] availability = new boolean[TOTAL_REQUESTS];
        int[] lateDays = new int[TOTAL_REQUESTS];

        String[] outcomes = new String[TOTAL_REQUESTS];
        double[] lateCharges = new double[TOTAL_REQUESTS];

        System.out.println("========================================");
        System.out.println("       BOOKBRIDGE LIBRARY SYSTEM");
        System.out.println("========================================");

        for (int i = 0; i < TOTAL_REQUESTS; i++) {

            System.out.println();
            System.out.println("Transaction " + (i + 1));
            System.out.println("----------------------------------------");

            memberIds[i] = readRequiredText(scanner, "Member ID: ");
            memberNames[i] = readRequiredText(scanner, "Member Name: ");
            membershipTypes[i] = readMembershipType(scanner);
            isbns[i] = readRequiredText(scanner, "ISBN: ");
            bookTitles[i] = readRequiredText(scanner, "Book Title: ");

            requestedDays[i] = readRequestedDays(scanner);
            availability[i] = readAvailability(scanner);
            lateDays[i] = readLateDays(scanner);

            outcomes[i] = classifyRequest(
                    membershipTypes[i],
                    requestedDays[i],
                    availability[i]
            );

            lateCharges[i] =
                    lateDays[i] * getDailyFine(membershipTypes[i]);

            printTransactionResult(
                    memberIds[i],
                    memberNames[i],
                    membershipTypes[i],
                    isbns[i],
                    bookTitles[i],
                    requestedDays[i],
                    availability[i],
                    lateDays[i],
                    outcomes[i],
                    lateCharges[i]
            );
        }

        printSummary(
                memberIds,
                memberNames,
                bookTitles,
                requestedDays,
                outcomes,
                lateCharges
        );

        scanner.close();
    }

    public static boolean validateMembershipType(String membershipType) {

        return membershipType.equals("STUDENT")
                || membershipType.equals("FACULTY")
                || membershipType.equals("GUEST");
    }

    public static int getMaximumDays(String membershipType) {

        switch (membershipType) {
            case "STUDENT":
                return STUDENT_MAX_DAYS;

            case "FACULTY":
                return FACULTY_MAX_DAYS;

            case "GUEST":
                return GUEST_MAX_DAYS;

            default:
                return 0;
        }
    }

   
    public static double getDailyFine(String membershipType) {

        switch (membershipType) {
            case "STUDENT":
                return STUDENT_FINE;

            case "FACULTY":
                return FACULTY_FINE;

            case "GUEST":
                return GUEST_FINE;

            default:
                return 0.0;
        }
    }

   
    public static String classifyRequest(
            String membershipType,
            int requestedDays,
            boolean available) {

        if (!available) {
            return "RESERVED";
        }

        int maximumDays = getMaximumDays(membershipType);

         if (requestedDays <= maximumDays) {
            return "APPROVED";
        }

        return "DENIED";
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
                    "Invalid input. This field cannot be blank."
            );
        }
    }

    public static String readMembershipType(Scanner scanner) {

        while (true) {

            System.out.print(
                    "Membership Type (STUDENT/FACULTY/GUEST): "
            );

            String membershipType =
                    scanner.nextLine().trim().toUpperCase();

            if (validateMembershipType(membershipType)) {
                return membershipType;
            }

            System.out.println(
                    "Invalid membership type. Please try again."
            );
        }
    }

    public static int readRequestedDays(Scanner scanner) {

        while (true) {

            System.out.print("Requested Loan Days (1-30): ");
            String input = scanner.nextLine().trim();

            try {

                int days = Integer.parseInt(input);

                if (days >= 1 && days <= 30) {
                    return days;
                }

                System.out.println(
                        "Invalid input. Requested days must be from 1 to 30."
                );

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid input. Please enter a whole number."
                );
            }
        }
    }


    public static boolean readAvailability(Scanner scanner) {

        while (true) {

            System.out.print("Is the book available? (YES/NO): ");

            String input =
                    scanner.nextLine().trim().toUpperCase();

            if (input.equals("YES")) {
                return true;
            }

            if (input.equals("NO")) {
                return false;
            }

            System.out.println(
                    "Invalid input. Please enter YES or NO."
            );
        }
    }

    public static int readLateDays(Scanner scanner) {

        while (true) {

            System.out.print("Late Days: ");
            String input = scanner.nextLine().trim();

            try {

                int days = Integer.parseInt(input);

                if (days >= 0) {
                    return days;
                }

                System.out.println(
                        "Invalid input. Late days cannot be negative."
                );

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid input. Please enter a whole number."
                );
            }
        }
    }

    public static void printTransactionResult(
            String memberId,
            String memberName,
            String membershipType,
            String isbn,
            String bookTitle,
            int requestedDays,
            boolean available,
            int lateDays,
            String outcome,
            double lateCharge) {

        System.out.println();
        System.out.println("---------- TRANSACTION RESULT ----------");

        System.out.println("Member ID       : " + memberId);
        System.out.println("Member Name     : " + memberName);
        System.out.println("Membership Type : " + membershipType);
        System.out.println("ISBN            : " + isbn);
        System.out.println("Book Title      : " + bookTitle);
        System.out.println("Requested Days  : " + requestedDays);
        System.out.println(
                "Available       : " + (available ? "YES" : "NO")
        );
        System.out.println("Late Days       : " + lateDays);
        System.out.println("Outcome         : " + outcome);

        System.out.printf(
                "Estimated Late Charge: PHP %.2f%n",
                lateCharge
        );

        System.out.println("----------------------------------------");
    }


    public static void printSummary(
            String[] memberIds,
            String[] memberNames,
            String[] bookTitles,
            int[] requestedDays,
            String[] outcomes,
            double[] lateCharges) {

        int approvedCount = 0;
        int reservedCount = 0;
        int deniedCount = 0;

        double totalLateCharge = 0.0;

        int greatestRequestIndex = 0;

        for (int i = 0; i < outcomes.length; i++) {

            switch (outcomes[i]) {

                case "APPROVED":
                    approvedCount++;
                    break;

                case "RESERVED":
                    reservedCount++;
                    break;

                case "DENIED":
                    deniedCount++;
                    break;
            }

            totalLateCharge += lateCharges[i];

            if (requestedDays[i]
                    > requestedDays[greatestRequestIndex]) {

                greatestRequestIndex = i;
            }
        }

        System.out.println();
        System.out.println("========================================");
        System.out.println("          BOOKBRIDGE SUMMARY");
        System.out.println("========================================");

        System.out.println(
                "Approved Requests : " + approvedCount
        );

        System.out.println(
                "Reserved Requests : " + reservedCount
        );

        System.out.println(
                "Denied Requests   : " + deniedCount
        );

        System.out.println();
        System.out.println("Request With Greatest Requested Days");

        System.out.println(
                "Member ID      : "
                        + memberIds[greatestRequestIndex]
        );

        System.out.println(
                "Member Name    : "
                        + memberNames[greatestRequestIndex]
        );

        System.out.println(
                "Book Title     : "
                        + bookTitles[greatestRequestIndex]
        );

        System.out.println(
                "Requested Days : "
                        + requestedDays[greatestRequestIndex]
        );

        System.out.printf(
                "%nTotal Estimated Late Charge: PHP %.2f%n",
                totalLateCharge
        );

        System.out.println("========================================");
    }
}