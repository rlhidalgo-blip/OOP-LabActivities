package campusconnect.service;

import campusconnect.dao.*;
import campusconnect.exception.PersistenceException;
import campusconnect.model.*;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** SQLite is authoritative; a fresh RequestService is restored for each operation. */
public class PersistentRequestService {
    private final Database database;
    private final RequestDao dao = new RequestDao();

    public PersistentRequestService(Path file) {
        database = new Database(file);
        database.initialize();
    }

    @FunctionalInterface
    private interface Work<T> { T run(Connection connection, RequestService service) throws SQLException; }

    private synchronized <T> T execute(Work<T> work) {
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                RequestService service = new RequestService();
                for (ServiceRequest request : dao.load(connection)) service.restore(request);
                T result = work.run(connection, service);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                try { connection.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                if (e instanceof SQLException) throw new PersistenceException("Database operation failed.", e);
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Database connection failed.", e);
        }
    }

    public ServiceRequest add(String studentId, String studentName, ServiceCategory category,
            int priority, String concern) {
        Student student = new Student(studentId, studentName);
        Instant now = Instant.now();
        ServiceRequest request = ServiceRequest.create("CC-" + UUID.randomUUID(), student,
                category, priority, concern, RequestStatus.PENDING, now, now);
        return add(request);
    }

    public ServiceRequest add(ServiceRequest request) {
        return execute((connection, service) -> {
            service.add(request);
            dao.insert(connection, request);
            return request;
        });
    }

    public List<ServiceRequest> listAll() { return execute((c, s) -> s.listAll()); }
    public List<ServiceRequest> sorted() { return execute((c, s) -> s.sorted()); }
    public List<ServiceRequest> findByStudent(String id) { return execute((c, s) -> s.findByStudent(id)); }
    public List<ServiceRequest> filter(ServiceCategory category, RequestStatus status) {
        return execute((c, s) -> s.filter(category, status));
    }
    public ServiceRequest find(String id) { return execute((c, s) -> s.find(id)); }
    public RequestSummary summary() { return execute((c, s) -> s.summary()); }

    public ServiceRequest updateStatus(String id, RequestStatus status) {
        return execute((connection, service) -> {
            service.updateStatus(id, status);
            ServiceRequest request = service.find(id);
            dao.updateStatus(connection, request);
            return request;
        });
    }

    public void remove(String id) {
        execute((connection, service) -> {
            service.remove(id);
            dao.delete(connection, id);
            return null;
        });
    }

    public Optional<ServiceRequest> processNext() {
        return execute((connection, service) -> {
            Optional<ServiceRequest> next = service.processNext();
            if (next.isPresent()) dao.updateStatus(connection, next.get());
            return next;
        });
    }
}
