package campusconnect.model;

import java.time.Instant;

public final class EnrollmentRequest extends ServiceRequest {
    public EnrollmentRequest(String ticketId, Student student, int priority, String concern,
            RequestStatus status, Instant createdAt, Instant updatedAt) {
        super(ticketId, student, priority, concern, status, createdAt, updatedAt);
    }
    @Override public ServiceCategory getCategory() { return ServiceCategory.ENROLLMENT; }
    @Override public int estimateDays() { return adjustedDays(3); }
}
