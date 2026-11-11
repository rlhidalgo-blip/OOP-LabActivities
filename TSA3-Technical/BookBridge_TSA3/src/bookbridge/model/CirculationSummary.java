package bookbridge.model;

import java.math.BigDecimal;
import java.util.Map;

public record CirculationSummary(int availableBooks, int checkedOutBooks,
        int activeLoans, int historicalLoans, int pendingReservations,
        Map<MembershipType, Long> activeLoansByMembershipType, BigDecimal estimatedFines) {
    public CirculationSummary {
        activeLoansByMembershipType = Map.copyOf(activeLoansByMembershipType);
    }
}
