package campusconnect.model;

import java.util.Map;

public record RequestSummary(int total, int distinctStudents,
        Map<ServiceCategory, Integer> byCategory,
        Map<RequestStatus, Integer> byStatus,
        double averageEstimatedDays) {
    public RequestSummary {
        byCategory = Map.copyOf(byCategory);
        byStatus = Map.copyOf(byStatus);
    }
}
