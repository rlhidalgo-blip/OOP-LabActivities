package campusconnect.app;

import campusconnect.model.*;
import campusconnect.service.PersistentRequestService;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

@SuppressWarnings({"serial", "this-escape"})
public class CampusConnectFrame extends JFrame {
    private final PersistentRequestService service;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] {"Ticket", "Student ID", "Name", "Category", "Priority", "Concern", "Days", "Status"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<DefaultTableModel> rowSorter = new TableRowSorter<>(tableModel);
    private final JTextArea report = new JTextArea();
    private final JLabel status = new JLabel("Ready");
    private final List<JButton> buttons = new ArrayList<>();
    private final JComboBox<String> category = new JComboBox<>(choices(ServiceCategory.values()));
    private final JComboBox<String> state = new JComboBox<>(choices(RequestStatus.values()));
    private String searchQuery = "";
    private boolean busy;

    public CampusConnectFrame(PersistentRequestService service) {
        super("CampusConnect Final");
        this.service = service;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 650);
        setLocationRelativeTo(null);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Requests", requestPanel());
        tabs.addTab("Reports", reportPanel());
        add(tabs, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSorter(rowSorter);
        category.addActionListener(e -> filter());
        state.addActionListener(e -> filter());
        report.setEditable(false);
        report.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        refresh();
    }

    private static <E extends Enum<E>> String[] choices(E[] values) {
        String[] result = new String[values.length + 1];
        result[0] = "All";
        for (int i = 0; i < values.length; i++) result[i + 1] = values[i].name();
        return result;
    }

    private JPanel requestPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel toolbar = new JPanel(new BorderLayout());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addButton(actions, "Add", this::addRequest);
        addButton(actions, "Search", this::search);
        addButton(actions, "List / Refresh", this::refresh);
        addButton(actions, "Update Status", this::updateStatus);
        addButton(actions, "Delete", this::delete);
        addButton(actions, "Process Next", this::processNext);
        addButton(actions, "Sort", this::sort);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Category:")); filters.add(category);
        filters.add(new JLabel("Status:")); filters.add(state);
        addButton(filters, "Filter", this::filter);
        toolbar.add(actions, BorderLayout.NORTH);
        toolbar.add(filters, BorderLayout.SOUTH);
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel reportPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addButton(top, "Refresh Report", this::refreshReport);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(report), BorderLayout.CENTER);
        return panel;
    }

    private void addButton(JPanel panel, String label, Runnable action) {
        JButton button = new JButton(label);
        button.addActionListener(e -> { if (!busy) action.run(); });
        buttons.add(button);
        panel.add(button);
    }

    private void setBusy(boolean value, String message) {
        busy = value;
        buttons.forEach(button -> button.setEnabled(!value));
        category.setEnabled(!value); state.setEnabled(!value);
        status.setText(message);
    }

    private <T> void work(String label, Callable<T> task, Consumer<T> done) {
        setBusy(true, label + "...");
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                try {
                    T result = get();
                    setBusy(false, label + " completed");
                    done.accept(result);
                }
                catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    status.setText("Failed: " + cause.getMessage());
                    JOptionPane.showMessageDialog(CampusConnectFrame.this, cause.getMessage(),
                            "CampusConnect error", JOptionPane.ERROR_MESSAGE);
                    setBusy(false, status.getText());
                }
            }
        }.execute();
    }

    private void fillRows(List<ServiceRequest> rows) {
        tableModel.setRowCount(0);
        for (ServiceRequest r : rows) tableModel.addRow(new Object[] { r.getTicketId(),
                r.getStudent().getId(), r.getStudent().getName(), r.getCategory(), r.getPriority(),
                r.getConcern(), r.estimateDays(), r.getStatus() });
    }
    private record Snapshot(List<ServiceRequest> rows, RequestSummary summary) { }
    private void refresh() {
        work("Refreshing", () -> new Snapshot(service.listAll(), service.summary()), snapshot -> {
            fillRows(snapshot.rows());
            showSummary(snapshot.summary());
        });
    }
    private void refreshReport() {
        work("Loading report", service::summary, this::showSummary);
    }
    private void showSummary(RequestSummary summary) {
        report.setText(
                "Total requests: " + summary.total() + "\nDistinct students: " + summary.distinctStudents()
                + "\nAverage estimated days: " + String.format("%.2f", summary.averageEstimatedDays())
                + "\n\nBy category:\n" + summary.byCategory() + "\n\nBy status:\n" + summary.byStatus());
    }

    private String selectedId() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a request first."); return null; }
        return (String) tableModel.getValueAt(table.convertRowIndexToModel(row), 0);
    }

    private void addRequest() {
        JTextField id = new JTextField(), name = new JTextField(), concern = new JTextField();
        JSpinner priority = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
        JComboBox<ServiceCategory> kind = new JComboBox<>(ServiceCategory.values());
        Object[] fields = {"Student ID", id, "Student name", name, "Category", kind,
                "Priority (1-5)", priority, "Concern", concern};
        if (JOptionPane.showConfirmDialog(this, fields, "Add request", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;
        work("Adding request", () -> service.add(id.getText(), name.getText(),
                (ServiceCategory) kind.getSelectedItem(), (Integer) priority.getValue(), concern.getText()),
                request -> refresh());
    }

    private void search() {
        String query = JOptionPane.showInputDialog(this, "Ticket ID or Student ID:");
        if (query == null) return;
        searchQuery = query.trim();
        filter();
    }

    private void filter() {
        ServiceCategory c = category.getSelectedIndex() == 0 ? null : ServiceCategory.valueOf((String) category.getSelectedItem());
        RequestStatus s = state.getSelectedIndex() == 0 ? null : RequestStatus.valueOf((String) state.getSelectedItem());
        rowSorter.setRowFilter(requestFilter(c, s, searchQuery));
    }

    public static RowFilter<DefaultTableModel, Integer> requestFilter(
            ServiceCategory category, RequestStatus status, String query) {
        String search = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return new RowFilter<>() {
            @Override public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> row) {
                if (category != null && row.getValue(3) != category) return false;
                if (status != null && row.getValue(7) != status) return false;
                return search.isEmpty()
                        || row.getStringValue(0).toLowerCase(Locale.ROOT).equals(search)
                        || row.getStringValue(1).toLowerCase(Locale.ROOT).equals(search);
            }
        };
    }

    private void sort() {
        rowSorter.setSortKeys(List.of(new RowSorter.SortKey(4, SortOrder.DESCENDING),
                new RowSorter.SortKey(6, SortOrder.ASCENDING),
                new RowSorter.SortKey(0, SortOrder.ASCENDING)));
    }

    private void updateStatus() {
        String id = selectedId(); if (id == null) return;
        RequestStatus next = (RequestStatus) JOptionPane.showInputDialog(this, "New status:", "Update status",
                JOptionPane.QUESTION_MESSAGE, null, RequestStatus.values(), RequestStatus.IN_PROGRESS);
        if (next == null) return;
        work("Updating status", () -> service.updateStatus(id, next), result -> refresh());
    }

    private void delete() {
        String id = selectedId(); if (id == null) return;
        if (JOptionPane.showConfirmDialog(this, "Delete " + id + "?", "Confirm deletion",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        work("Deleting request", () -> { service.remove(id); return id; }, result -> refresh());
    }

    private void processNext() {
        work("Processing next", service::processNext, result -> {
            if (result.isEmpty()) JOptionPane.showMessageDialog(this, "No pending requests.");
            refresh();
        });
    }
}
