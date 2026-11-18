package campusconnect.dao;

import campusconnect.exception.PersistenceException;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;

public class Database {
    private final Path file;

    public Database(Path file) { this.file = file.toAbsolutePath(); }

    public Connection open() throws SQLException {
        try { Files.createDirectories(file.getParent()); }
        catch (IOException e) { throw new PersistenceException("Cannot create database folder.", e); }
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    public void initialize() {
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            String sql = Files.readString(Path.of("schema.sql"));
            for (String part : sql.split(";")) {
                if (!part.isBlank()) statement.execute(part);
            }
        } catch (SQLException | IOException e) {
            throw new PersistenceException("Database setup failed.", e);
        }
    }
}
