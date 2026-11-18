package bookbridge.ui;

import bookbridge.exception.PersistenceException;
import bookbridge.model.LibrarySnapshot;
import bookbridge.service.PersistentLibraryService;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/** The only window. All widgets are read or changed on Swing's event thread. */
public final class BookBridgeFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private final transient PersistentLibraryService service;
    private final CatalogPanel catalog;
    private final MembersPanel members;
    private final CirculationPanel circulation;
    private final ReservationsPanel reservations;
    private final ReportsPanel reports;
    private final JLabel status = new JLabel("Starting BookBridge...");
    private final JProgressBar progress = new JProgressBar();
    private final transient List<JButton> actions = new ArrayList<>();
    private boolean busy;
    private boolean stale = true;

    public BookBridgeFrame(PersistentLibraryService service) {
        super("BookBridge Final");
        this.service = service;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(850, 550));
        setSize(1050, 680);
        setLocationRelativeTo(null);
        catalog = new CatalogPanel(this);
        members = new MembersPanel(this);
        circulation = new CirculationPanel(this);
        reservations = new ReservationsPanel(this);
        reports = new ReportsPanel(this);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Catalog", catalog);
        tabs.addTab("Members", members);
        tabs.addTab("Circulation", circulation);
        tabs.addTab("Reservations", reservations);
        tabs.addTab("Reports", reports);
        add(tabs, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.add(status, BorderLayout.CENTER);
        progress.setIndeterminate(true);
        progress.setVisible(false);
        footer.add(progress, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    PersistentLibraryService service() { return service; }

    JButton action(String title) {
        JButton button = new JButton(title);
        actions.add(button);
        return button;
    }

    void refresh() { load("Library refreshed", service::refresh); }

    public void start() {
        setVisible(true);
        load("Library ready", () -> {
            service.initialize();
            return service.refresh();
        });
    }

    void change(String message, Callable<LibrarySnapshot> task) {
        if (stale) {
            JOptionPane.showMessageDialog(this, "Refresh the library before changing records.");
            return;
        }
        load(message, task);
    }

    private void load(String message, Callable<LibrarySnapshot> task) {
        if (busy) return;
        busy = true;
        setActionsEnabled(false);
        status.setText("Working...");
        progress.setVisible(true);
        new SwingWorker<LibrarySnapshot, Void>() {
            @Override protected LibrarySnapshot doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                busy = false;
                progress.setVisible(false);
                try {
                    LibrarySnapshot snapshot = get();
                    catalog.show(snapshot);
                    members.show(snapshot);
                    circulation.show(snapshot);
                    reservations.show(snapshot);
                    reports.show(snapshot);
                    stale = false;
                    status.setText(message);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    status.setText("Operation interrupted. Refresh to continue.");
                    stale = true;
                } catch (ExecutionException error) {
                    Throwable cause = error.getCause();
                    if (cause instanceof PersistenceException) stale = true;
                    String detail = cause.getMessage() == null ? cause.toString() : cause.getMessage();
                    status.setText("Operation failed: " + detail);
                    JOptionPane.showMessageDialog(BookBridgeFrame.this, detail, "BookBridge error",
                            JOptionPane.ERROR_MESSAGE);
                }
                setActionsEnabled(true);
            }
        }.execute();
    }

    private void setActionsEnabled(boolean enabled) {
        for (JButton button : actions) {
            // Refresh buttons remain useful after a database error.
            button.setEnabled(enabled && (!stale || "Refresh".equals(button.getText())));
        }
    }

    /** Small pop-up form used by the three circulation/catalog actions. */
    String[] form(String title, String... labels) {
        JPanel fields = new JPanel(new java.awt.GridLayout(labels.length, 2, 8, 8));
        JTextField[] inputs = new JTextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            fields.add(new JLabel(labels[i]));
            inputs[i] = new JTextField(22);
            fields.add(inputs[i]);
        }
        if (JOptionPane.showConfirmDialog(this, fields, title, JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return null;
        String[] result = new String[labels.length];
        for (int i = 0; i < labels.length; i++) result[i] = inputs[i].getText();
        return result;
    }
}
