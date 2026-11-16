package bookbridge.tests;

import bookbridge.model.LibrarySnapshot;
import bookbridge.persistence.Database;
import bookbridge.service.PersistentLibraryService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/** Checks the worker/EDT boundary without opening a window. */
public final class SwingWorkerTest {
    private SwingWorkerTest() { }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("bookbridge-worker-");
        PersistentLibraryService service = new PersistentLibraryService(
                new Database(directory.resolve("library.db"), Path.of("schema.sql")));
        CountDownLatch completed = new CountDownLatch(1);
        AtomicBoolean backgroundWasOffEdt = new AtomicBoolean();
        AtomicBoolean doneWasOnEdt = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> new SwingWorker<LibrarySnapshot, Void>() {
            @Override protected LibrarySnapshot doInBackground() {
                backgroundWasOffEdt.set(!SwingUtilities.isEventDispatchThread());
                service.initialize();
                return service.refresh();
            }
            @Override protected void done() {
                doneWasOnEdt.set(SwingUtilities.isEventDispatchThread());
                try {
                    if (get().books().size() != 3) throw new AssertionError("Sample books missing");
                } catch (Throwable error) { failure.set(error); }
                completed.countDown();
            }
        }.execute());
        if (!completed.await(15, TimeUnit.SECONDS)) throw new AssertionError("Worker did not finish");
        if (failure.get() != null) throw new AssertionError("Worker failed", failure.get());
        if (!backgroundWasOffEdt.get() || !doneWasOnEdt.get()) {
            throw new AssertionError("SwingWorker used the wrong thread");
        }
        System.out.println("TSA4 SwingWorker boundary: 2 passed, 0 failed");
    }
}
