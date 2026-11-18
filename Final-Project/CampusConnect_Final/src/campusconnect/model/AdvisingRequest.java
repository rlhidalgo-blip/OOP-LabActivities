package campusconnect.model;

import java.time.Instant;

public final class AdvisingRequest extends ServiceRequest {
    public AdvisingRequest(String ticketId, Student student, int priority, String concern,
            RequestStatus status, Instant createdAt, Instant updatedAt) {
        super(ticketId, student, priority, concern, status, createdAt, updatedAt);
    }
    @Override public ServiceCategory getCategory() { return ServiceCategory.ADVISING; }
    @Override public int estimateDays() { return adjustedDays(4); }
}
