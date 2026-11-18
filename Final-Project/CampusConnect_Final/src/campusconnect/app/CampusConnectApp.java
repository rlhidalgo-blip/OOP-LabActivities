package campusconnect.app;

import campusconnect.service.PersistentRequestService;
import java.nio.file.Path;
import javax.swing.*;

public class CampusConnectApp {
    public static void main(String[] args) {
        Path database = Path.of(args.length == 0 ? "data/campusconnect.db" : args[0]);
        try {
            PersistentRequestService service = new PersistentRequestService(database);
            SwingUtilities.invokeLater(() -> new CampusConnectFrame(service).setVisible(true));
        } catch (RuntimeException e) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    e.getMessage(), "Startup error", JOptionPane.ERROR_MESSAGE));
        }
    }
}
