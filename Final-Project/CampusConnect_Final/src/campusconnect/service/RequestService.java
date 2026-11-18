package campusconnect.service;

import campusconnect.exception.DuplicateTicketException;
import campusconnect.exception.InvalidRequestException;
import campusconnect.exception.RequestNotFoundException;
import campusconnect.model.*;
import java.time.Instant;
import java.util.*;

/** In-memory rules for one database transaction. Database order is restored before use. */
public class RequestService {
    private final Map<String, ServiceRequest> requests = new LinkedHashMap<>();
    private final Queue<String> pending = new ArrayDeque<>();
    private final Set<String> students = new HashSet<>();

    public void restore(ServiceRequest request) {
        add(request);
    }

    public void add(ServiceRequest request) {
        if (request == null) throw new InvalidRequestException("Request is required.");
        if (requests.putIfAbsent(request.getTicketId(), request) != null)
            throw new DuplicateTicketException("Ticket already exists: " + request.getTicketId());
        students.add(request.getStudent().getId());
        if (request.getStatus() == RequestStatus.PENDING) pending.add(request.getTicketId());
    }

    public ServiceRequest find(String ticketId) {
        return require(requests, ticketId);
    }

    private static <K, V> V require(Map<K, V> map, K key) {
        V value = map.get(key);
        if (value == null) throw new RequestNotFoundException("Ticket not found: " + key);
        return value;
    }

    public List<ServiceRequest> listAll() { return List.copyOf(requests.values()); }
    public List<ServiceRequest> findByStudent(String id) {
        return requests.values().stream().filter(r -> r.getStudent().getId().equalsIgnoreCase(id)).toList();
    }
    public List<ServiceRequest> filter(ServiceCategory category, RequestStatus status) {
        return requests.values().stream().filter(r -> category == null || r.getCategory() == category)
                .filter(r -> status == null || r.getStatus() == status).toList();
    }
    public List<ServiceRequest> sorted() {
        return requests.values().stream().sorted(Comparator.comparingInt(ServiceRequest::getPriority).reversed()
                .thenComparingInt(ServiceRequest::estimateDays).thenComparing(ServiceRequest::getTicketId)).toList();
    }
    public void updateStatus(String id, RequestStatus status) {
        ServiceRequest request = find(id);
        request.transitionTo(status, Instant.now());
        if (status != RequestStatus.PENDING) pending.remove(id);
    }
    public void remove(String id) {
        find(id);
        requests.remove(id);
        pending.remove(id);
        students.clear();
        requests.values().forEach(r -> students.add(r.getStudent().getId()));
    }
    public Optional<ServiceRequest> processNext() {
        String id = pending.poll();
        if (id == null) return Optional.empty();
        ServiceRequest request = find(id);
        request.transitionTo(RequestStatus.IN_PROGRESS, Instant.now());
        return Optional.of(request);
    }
    public RequestSummary summary() {
        Map<ServiceCategory, Integer> categories = new EnumMap<>(ServiceCategory.class);
        Map<RequestStatus, Integer> statuses = new EnumMap<>(RequestStatus.class);
        int days = 0;
        for (ServiceRequest request : requests.values()) {
            categories.merge(request.getCategory(), 1, Integer::sum);
            statuses.merge(request.getStatus(), 1, Integer::sum);
            days += request.estimateDays();
        }
        return new RequestSummary(requests.size(), students.size(), categories, statuses,
                requests.isEmpty() ? 0 : (double) days / requests.size());
    }
}
