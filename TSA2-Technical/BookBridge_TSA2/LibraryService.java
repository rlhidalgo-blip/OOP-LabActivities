public class LibraryService {

    public String classifyRequest(Member member,
                                  LibraryBook book,
                                  int requestedDays) {

        if (!book.isAvailable()) {
            return "RESERVED";
        }

        if (requestedDays <= member.getMaximumLoanDays()) {
            return "APPROVED";
        }

        return "DENIED";
    }
}