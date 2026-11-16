package bookbridge.ui;

import bookbridge.model.LibrarySnapshot;
import bookbridge.model.Reservation;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

final class ReservationsPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final BookBridgeFrame frame;
    private final DefaultTableModel rows = new DefaultTableModel(
            new String[] {"Queue", "Reservation ID", "Member", "ISBN", "Days"}, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(rows);
    private transient List<Reservation> reservations = List.of();

    ReservationsPanel(BookBridgeFrame frame) {
        super(new BorderLayout(8, 8));
        this.frame = frame;
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton reserve = frame.action("Reserve Book");
        reserve.addActionListener(e -> {
            String[] values = frame.form("Reserve Book", "Member ID", "ISBN", "Loan days");
            if (values != null) frame.change("Reservation added", () -> frame.service()
                    .reserveBook(values[0], values[1], Integer.parseInt(values[2].strip())).snapshot());
        });
        JButton cancel = frame.action("Cancel Selected Reservation");
        cancel.addActionListener(e -> cancelSelected());
        JButton process = frame.action("Process Next Reservation");
        process.addActionListener(e -> frame.change("Reservation queue checked", () ->
                frame.service().processNextReservation().snapshot()));
        JButton refresh = frame.action("Refresh");
        refresh.addActionListener(e -> frame.refresh());
        top.add(reserve); top.add(cancel); top.add(process); top.add(refresh);
        add(top, BorderLayout.NORTH);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void cancelSelected() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            JOptionPane.showMessageDialog(this, "Select a pending reservation first.");
            return;
        }
        Reservation reservation = reservations.get(table.convertRowIndexToModel(selected));
        if (JOptionPane.showConfirmDialog(this, "Cancel reservation " + reservation.reservationId() + "?",
                "Confirm cancellation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            frame.change("Reservation cancelled", () ->
                    frame.service().cancelReservation(reservation.reservationId()).snapshot());
        }
    }

    void show(LibrarySnapshot snapshot) {
        reservations = snapshot.reservations();
        rows.setRowCount(0);
        for (int i = 0; i < reservations.size(); i++) {
            Reservation reservation = reservations.get(i);
            rows.addRow(new Object[] {i + 1, reservation.reservationId(),
                    reservation.memberId(), reservation.isbn(), reservation.requestedDays()});
        }
    }
}
