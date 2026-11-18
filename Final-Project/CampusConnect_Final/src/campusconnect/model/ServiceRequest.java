package campusconnect.model;

import campusconnect.exception.InvalidRequestException;
import java.time.Instant;

public abstract class ServiceRequest implements Prioritizable {
    private final String ticketId;
    private final Student student;
    private final int priority;
    private final String concern;
    private final Instant createdAt;
    private RequestStatus status;
    private Instant updatedAt;

    protected ServiceRequest(String ticketId, Student student, int priority, String concern,
            RequestStatus status, Instant createdAt, Instant updatedAt) {
        if (ticketId == null || ticketId.trim().isEmpty()) {
            throw new InvalidRequestException("Ticket ID is required.");
        }
        if (student == null || status == null || createdAt == null || updatedAt == null) {
            throw new InvalidRequestException("Student, status, and timestamps are required.");
        }
        if (priority < 1 || priority > 5) {
            throw new InvalidRequestException("Priority must be from 1 to 5.");
        }
        if (concern == null || concern.trim().isEmpty()) {
            throw new InvalidRequestException("Concern is required.");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new InvalidRequestException("Update time cannot precede creation time.");
        }
        this.ticketId = ticketId.trim();
        this.student = student;
        this.priority = priority;
        this.concern = concern.trim();
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ServiceRequest create(String ticketId, Student student, ServiceCategory category,
            int priority, String concern, RequestStatus status, Instant createdAt, Instant updatedAt) {
        if (category == null) {
            throw new InvalidRequestException("Category is required.");
        }
        return switch (category) {
            case ENROLLMENT -> new EnrollmentRequest(ticketId, student, priority, concern, status, createdAt, updatedAt);
            case DOCUMENT -> new DocumentRequest(ticketId, student, priority, concern, status, createdAt, updatedAt);
            case PAYMENT -> new PaymentConcern(ticketId, student, priority, concern, status, createdAt, updatedAt);
            case ADVISING -> new AdvisingRequest(ticketId, student, priority, concern, status, createdAt, updatedAt);
        };
    }

    protected final int adjustedDays(int baseDays) {
        return priority == 5 ? Math.max(1, baseDays - 1) : baseDays;
    }

    public abstract ServiceCategory getCategory();
    public abstract int estimateDays();

    public void transitionTo(RequestStatus next, Instant when) {
        if (next == null || when == null) {
            throw new InvalidRequestException("Status and update time are required.");
        }
        boolean allowed = (status == RequestStatus.PENDING
                && (next == RequestStatus.IN_PROGRESS || next == RequestStatus.CANCELLED))
                || (status == RequestStatus.IN_PROGRESS
                && (next == RequestStatus.COMPLETED || next == RequestStatus.CANCELLED));
        if (!allowed) {
            throw new InvalidRequestException("Cannot change " + status + " to " + next + ".");
        }
        if (when.isBefore(createdAt)) {
            throw new InvalidRequestException("Update time cannot precede creation time.");
        }
        status = next;
        updatedAt = when;
    }

    public String getTicketId() { return ticketId; }
    public Student getStudent() { return student; }
    @Override public int getPriority() { return priority; }
    public String getConcern() { return concern; }
    public RequestStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
