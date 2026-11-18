package bookbridge.ui;

import bookbridge.model.CirculationSummary;
import bookbridge.model.LibrarySnapshot;
import bookbridge.model.LoanRecord;
import bookbridge.model.MembershipType;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

final class ReportsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JLabel books = new JLabel();
    private final JLabel student = new JLabel();
    private final JLabel faculty = new JLabel();
    private final JLabel guest = new JLabel();
    private final JLabel fines = new JLabel();
    private final DefaultTableModel rows = new DefaultTableModel(
            new String[] {"Loan ID", "Member", "ISBN", "Due", "Status", "Fine (PHP)"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };

    ReportsPanel(BookBridgeFrame frame) {
        super(new BorderLayout(8, 8));
        JPanel top = new JPanel(new BorderLayout());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refresh = frame.action("Refresh");
        refresh.addActionListener(e -> frame.refresh());
        controls.add(refresh);
        top.add(controls, BorderLayout.NORTH);
        JPanel summary = new JPanel(new GridLayout(3, 2, 8, 8));
        summary.add(books); summary.add(fines);
        summary.add(student); summary.add(faculty); summary.add(guest);
        top.add(summary, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        JTable table = new JTable(rows);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    void show(LibrarySnapshot snapshot) {
        CirculationSummary summary = snapshot.summary();
        books.setText("Available: " + summary.availableBooks() + "    Checked out: " + summary.checkedOutBooks());
        fines.setText("Estimated active late fines: PHP " + summary.estimatedFines());
        student.setText("Student active loans: " + summary.activeLoansByMembershipType().get(MembershipType.STUDENT));
        faculty.setText("Faculty active loans: " + summary.activeLoansByMembershipType().get(MembershipType.FACULTY));
        guest.setText("Guest active loans: " + summary.activeLoansByMembershipType().get(MembershipType.GUEST));
        rows.setRowCount(0);
        LocalDate today = LocalDate.now();
        for (LoanRecord loan : snapshot.sortedLoans()) {
            rows.addRow(new Object[] {loan.loanId(), loan.member().getMemberId(), loan.isbn(),
                    loan.dueDate(), loan.status(), loan.calculateLateCharge(today)});
        }
    }
}
