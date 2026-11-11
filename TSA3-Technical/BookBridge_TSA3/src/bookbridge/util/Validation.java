package bookbridge.util;

public final class Validation {
    private Validation() { }

    public static String requiredText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank.");
        }
        return value.strip();
    }
}
