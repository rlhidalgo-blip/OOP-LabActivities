package bookbridge.ui;

import bookbridge.model.LibraryBook;
import bookbridge.model.LibrarySnapshot;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

final class CatalogPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final BookBridgeFrame frame;
    private final JTextField search = new JTextField(22);
    private final DefaultTableModel rows = new DefaultTableModel(new String[] {"ISBN", "Title", "Availability"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private transient List<LibraryBook> books = List.of();

    CatalogPanel(BookBridgeFrame frame) {
        super(new BorderLayout(8, 8));
        this.frame = frame;
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = frame.action("Add Book");
        add.addActionListener(e -> {
            String[] input = frame.form("Add Book", "ISBN", "Title");
            if (input != null) frame.change("Book added", () ->
                    frame.service().addBook(new LibraryBook(input[0], input[1])).snapshot());
        });
        JButton find = frame.action("Search");
        find.addActionListener(e -> render());
        JButton refresh = frame.action("Refresh");
        refresh.addActionListener(e -> frame.refresh());
        top.add(add); top.add(search); top.add(find); top.add(refresh);
        add(top, BorderLayout.NORTH);
        JTable table = new JTable(rows);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    void show(LibrarySnapshot snapshot) { books = snapshot.books(); render(); }
    private void render() {
        rows.setRowCount(0);
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        for (LibraryBook book : books) {
            if (query.isEmpty() || book.isbn().toLowerCase(Locale.ROOT).contains(query)
                    || book.title().toLowerCase(Locale.ROOT).contains(query)) {
                rows.addRow(new Object[] {book.isbn(), book.title(), book.status()});
            }
        }
    }
}
