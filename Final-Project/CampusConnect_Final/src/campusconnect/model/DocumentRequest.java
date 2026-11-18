package campusconnect.model;

import java.time.Instant;

public final class DocumentRequest extends ServiceRequest {
    public DocumentRequest(String ticketId, Student student, int priority, String concern,
            RequestStatus status, Instant createdAt, Instant updatedAt) {
        super(ticketId, student, priority, concern, status, createdAt, updatedAt);
    }
    @Override public ServiceCategory getCategory() { return ServiceCategory.DOCUMENT; }
    @Override public int estimateDays() { return adjustedDays(2); }
}
