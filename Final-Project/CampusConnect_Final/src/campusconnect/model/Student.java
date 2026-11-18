package campusconnect.model;

import campusconnect.exception.InvalidRequestException;

public final class Student {
    private final String id;
    private final String name;

    public Student(String id, String name) {
        this.id = required(id, "Student ID");
        this.name = required(name, "Student name");
    }

    private static String required(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidRequestException(label + " is required.");
        }
        return value.trim();
    }

    public String getId() { return id; }
    public String getName() { return name; }
}
