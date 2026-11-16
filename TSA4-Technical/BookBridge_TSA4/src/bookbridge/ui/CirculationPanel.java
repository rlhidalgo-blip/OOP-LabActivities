package bookbridge.ui;

import bookbridge.model.LibrarySnapshot;
import bookbridge.model.LoanRecord;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

final class CirculationPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final BookBridgeFrame frame;
    private final DefaultTableModel rows = new DefaultTableModel(
            new String[] {"Loan ID", "Borrower", "ISBN", "Issued", "Due", "Status", "Fine (PHP)"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(rows);
    private transient List<LoanRecord> loans = List.of();

    CirculationPanel(BookBridgeFrame frame) {
        super(new BorderLayout(8, 8));
        this.frame = frame;
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton issue = frame.action("Issue Loan");
        issue.addActionListener(e -> {
            String[] values = frame.form("Issue Loan", "Member ID", "ISBN", "Loan days");
            if (values != null) frame.change("Loan issued", () -> frame.service()
                    .issueLoan(values[0], values[1], Integer.parseInt(values[2].strip())).snapshot());
        });
        JButton returned = frame.action("Return Selected Loan");
        returned.addActionListener(e -> returnSelected());
        JButton refresh = frame.action("Refresh");
        refresh.addActionListener(e -> frame.refresh());
        top.add(issue); top.add(returned); top.add(refresh);
        add(top, BorderLayout.NORTH);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void returnSelected() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Select an active loan first.");
            return;
        }
        LoanRecord loan = loans.get(table.convertRowIndexToModel(selected));
        if (loan.isReturned()) {
            JOptionPane.showMessageDialog(this, "This loan has already been returned.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Return loan " + loan.loanId() + "?",
                "Confirm return", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            frame.change("Book returned; process reservations manually", () ->
                    frame.service().returnBook(loan.loanId()).snapshot());
        }
    }

    void show(LibrarySnapshot snapshot) {
        loans = snapshot.loans();
        rows.setRowCount(0);
        LocalDate today = LocalDate.now();
        for (LoanRecord loan : loans) {
            rows.addRow(new Object[] {loan.loanId(), loan.member().getMemberId(), loan.isbn(),
                    loan.issueDate(), loan.dueDate(), loan.status(), loan.calculateLateCharge(today)});
        }
    }
}
