public class Member {

    private String name;
    private Book borrowedBook;

    public Member(String name) {
        this.name = name;
        this.borrowedBook = null;
    }

    public void borrow(Book book) {
        if (book.isAvailable()) {
            borrowedBook = book;
            book.borrowBook();

            System.out.println(
                name + " successfully borrowed \"" + book.getTitle() + "\"."
            );
        } else {
            System.out.println(
                name + " cannot borrow \"" + book.getTitle()
                + "\" because it is already unavailable."
            );
        }
    }

    public void displayBorrowedBook() {
        if (borrowedBook != null) {
            System.out.println(
                name + " borrowed: " + borrowedBook.getTitle()
            );
        } else {
            System.out.println(
                name + " has no borrowed book."
            );
        }
    }
}