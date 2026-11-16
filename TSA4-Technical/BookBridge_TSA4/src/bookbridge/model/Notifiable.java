package bookbridge.model;

/** Presentation decides how to deliver this message; the model performs no I/O. */
public interface Notifiable {
    String notificationMessage();
}
