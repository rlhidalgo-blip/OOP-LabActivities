package bookbridge.persistence;

import bookbridge.exception.PersistenceException;
import bookbridge.model.FacultyMember;
import bookbridge.model.GuestMember;
import bookbridge.model.LibraryBook;
import bookbridge.model.StudentMember;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.sqlite.SQLiteConfig;

/** One connection per task; no connection or service is shared between workers. */
public final class Database {
    private final Path file;
    private final Path schema;

    public Database(Path file, Path schema) {
        this.file = file.toAbsolutePath();
        this.schema = schema.toAbsolutePath();
    }

    public Connection open() throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.setBusyTimeout(3000);
        config.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file, config.toProperties());
        try {
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery("PRAGMA foreign_keys")) {
                if (!result.next() || result.getInt(1) != 1) {
                    throw new SQLException("SQLite foreign keys could not be enabled.");
                }
            }
            return connection;
        } catch (SQLException error) {
            try { connection.close(); } catch (SQLException close) { error.addSuppressed(close); }
            throw error;
        }
    }

    public void initialize() {
        try {
            Files.createDirectories(file.getParent());
            try (Connection connection = open()) {
                connection.setAutoCommit(false);
                try {
                    int version;
                    try (Statement statement = connection.createStatement();
                            ResultSet result = statement.executeQuery("PRAGMA user_version")) {
                        version = result.next() ? result.getInt(1) : -1;
                    }
                    if (version == 0) {
                        try (Statement statement = connection.createStatement();
                                ResultSet result = statement.executeQuery(
                                        "SELECT count(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")) {
                            if (result.next() && result.getInt(1) != 0) {
                                throw new SQLException("Existing database has no BookBridge schema version.");
                            }
                        }
                        String sql = Files.readString(schema);
                        try (Statement statement = connection.createStatement()) {
                            for (String part : sql.split(";")) {
                                if (!part.isBlank()) statement.execute(part);
                            }
                        }
                        LibraryDao dao = new LibraryDao(connection);
                        dao.insertMember(new StudentMember("S100", "Sample Student"));
                        dao.insertMember(new FacultyMember("F100", "Sample Faculty"));
                        dao.insertMember(new GuestMember("G100", "Sample Guest"));
                        dao.insertBook(new LibraryBook("9780000000001", "Java Collections"));
                        dao.insertBook(new LibraryBook("9780000000002", "Object-Oriented Java"));
                        dao.insertBook(new LibraryBook("9780000000003", "Library Systems"));
                        try (Statement statement = connection.createStatement()) {
                            statement.execute("PRAGMA user_version = 1");
                        }
                    } else if (version != 1) {
                        throw new SQLException("Unsupported BookBridge database version: " + version);
                    }
                    connection.commit();
                } catch (Exception error) {
                    try { connection.rollback(); } catch (SQLException rollback) { error.addSuppressed(rollback); }
                    throw error;
                }
            }
        } catch (SQLException | IOException error) {
            throw new PersistenceException("Could not initialize BookBridge database.", error);
        } catch (RuntimeException error) {
            throw new PersistenceException("Invalid BookBridge database.", error);
        } catch (Exception error) {
            throw new PersistenceException("Could not initialize BookBridge database.", error);
        }
    }
}
