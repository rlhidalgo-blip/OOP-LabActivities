package campusconnect.dao;

import campusconnect.exception.*;
import campusconnect.model.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public class RequestDao {
    public List<ServiceRequest> load(Connection connection) throws SQLException {
        String sql = "SELECT r.*, s.student_name FROM service_request r JOIN students s ON s.student_id=r.student_id ORDER BY r.queue_no";
        List<ServiceRequest> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                Student student = new Student(rows.getString("student_id"), rows.getString("student_name"));
                result.add(ServiceRequest.create(rows.getString("ticket_id"), student,
                        ServiceCategory.valueOf(rows.getString("category")), rows.getInt("priority"),
                        rows.getString("concern"), RequestStatus.valueOf(rows.getString("status")),
                        Instant.parse(rows.getString("created_at")), Instant.parse(rows.getString("updated_at"))));
            }
        }
        return result;
    }

    public void insert(Connection connection, ServiceRequest request) throws SQLException {
        String studentSql = "INSERT OR IGNORE INTO students(student_id,student_name) VALUES (?,?)";
        try (PreparedStatement statement = connection.prepareStatement(studentSql)) {
            statement.setString(1, request.getStudent().getId());
            statement.setString(2, request.getStudent().getName());
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT student_name FROM students WHERE student_id=?")) {
            statement.setString(1, request.getStudent().getId());
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next() || !rows.getString(1).equals(request.getStudent().getName()))
                    throw new InvalidRequestException("Student ID belongs to a different name.");
            }
        }
        String sql = "INSERT INTO service_request(ticket_id,student_id,category,priority,concern,status,queue_no,created_at,updated_at) "
                + "VALUES (?,?,?,?,?,?,(SELECT COALESCE(MAX(queue_no),0)+1 FROM service_request),?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, request.getTicketId());
            statement.setString(2, request.getStudent().getId());
            statement.setString(3, request.getCategory().name());
            statement.setInt(4, request.getPriority());
            statement.setString(5, request.getConcern());
            statement.setString(6, request.getStatus().name());
            statement.setString(7, request.getCreatedAt().toString());
            statement.setString(8, request.getUpdatedAt().toString());
            if (statement.executeUpdate() != 1) throw new SQLException("Insert affected no rows.");
        }
    }

    public void updateStatus(Connection connection, ServiceRequest request) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE service_request SET status=?,updated_at=? WHERE ticket_id=?")) {
            statement.setString(1, request.getStatus().name());
            statement.setString(2, request.getUpdatedAt().toString());
            statement.setString(3, request.getTicketId());
            if (statement.executeUpdate() != 1) throw new RequestNotFoundException("Ticket disappeared: " + request.getTicketId());
        }
    }

    public void delete(Connection connection, String ticketId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM service_request WHERE ticket_id=?")) {
            statement.setString(1, ticketId);
            if (statement.executeUpdate() != 1) throw new RequestNotFoundException("Ticket disappeared: " + ticketId);
        }
    }
}
