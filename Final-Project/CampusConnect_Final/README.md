# CampusConnect Final

CampusConnect is a Java desktop application for campus staff to manage student service requests. Staff can add, find, sort, filter, update, delete, and process requests. SQLite keeps records between launches.

## Requirements

- JDK 17 or newer
- SQLite JDBC driver `sqlite-jdbc-3.50.3.0.jar` (the `pom.xml` declares the Maven dependency)
- A desktop environment for the Swing window

From this project folder, obtain the driver with Maven if it is installed:

```sh
mvn dependency:copy-dependencies -DoutputDirectory=lib
```

The driver may also be downloaded from Maven Central and placed at `lib/sqlite-jdbc-3.50.3.0.jar`. The driver and local database are excluded from Git.

## Compile and run

Run these commands from `Final-Project/CampusConnect_Final/` on macOS or Linux:

```sh
mkdir -p build/classes
javac --release 17 -cp lib/sqlite-jdbc-3.50.3.0.jar -d build/classes $(find src -name '*.java')
java -cp build/classes:lib/sqlite-jdbc-3.50.3.0.jar campusconnect.app.CampusConnectApp
```

The default database is `data/campusconnect.db`. To use a different database, pass its path as an argument after `campusconnect.app.CampusConnectApp`. The application creates the schema on first launch and does not reset existing records. Run from the project folder so it can read `schema.sql`.

## Test

```sh
javac --release 17 -cp lib/sqlite-jdbc-3.50.3.0.jar -d build/classes $(find src tests -name '*.java')
java -Djava.awt.headless=true -cp build/classes:lib/sqlite-jdbc-3.50.3.0.jar CampusConnectFinalTest
```

The test program creates temporary databases and reports the pass/fail count. It does not use the application database. On JDK 24 or newer, the SQLite driver may print a native-access warning; this does not affect Java 17 compilation.

## Rules

The categories use these base estimates: Enrollment 3, Document 2, Payment 1, and Advising 4 working days. Priority 5 subtracts one day, with a minimum of one day. Pending requests are processed in entry order. Statuses may move from Pending to In Progress or Cancelled, and from In Progress to Completed or Cancelled. Completed and Cancelled requests cannot be reopened.
