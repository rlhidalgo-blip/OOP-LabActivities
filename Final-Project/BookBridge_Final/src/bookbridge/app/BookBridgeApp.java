package bookbridge.app;

import bookbridge.persistence.Database;
import bookbridge.service.PersistentLibraryService;
import bookbridge.ui.BookBridgeFrame;
import java.nio.file.Path;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class BookBridgeApp {
    private BookBridgeApp() { }

    public static void main(String[] args) {
        Path databaseFile = args.length == 0 ? Path.of("data", "bookbridge.db") : Path.of(args[0]);
        Database database = new Database(databaseFile, Path.of("schema.sql"));
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception appearanceError) {
                System.err.println("System appearance unavailable; using Swing default: "
                        + appearanceError.getMessage());
            }
            new BookBridgeFrame(new PersistentLibraryService(database)).start();
        });
    }
}
