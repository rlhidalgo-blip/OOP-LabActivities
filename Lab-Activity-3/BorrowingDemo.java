public class BorrowingDemo {

    public static void main(String[] args) {

        Book book = new Book("Introduction to Java");

        Member member1 = new Member("Juan");
        Member member2 = new Member("Maria");

        System.out.println("=== FIRST BORROWING ATTEMPT ===");

        member1.borrow(book);
        member1.displayBorrowedBook();

        System.out.println("Book available: " + book.isAvailable());

        System.out.println();

        System.out.println("=== SECOND BORROWING ATTEMPT ===");

        member2.borrow(book);
        member2.displayBorrowedBook();

        System.out.println("Book available: " + book.isAvailable());
    }
}