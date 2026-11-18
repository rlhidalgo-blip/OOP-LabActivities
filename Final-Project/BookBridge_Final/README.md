# BookBridge Final Project

BookBridge Final is a desktop library application for a librarian. It brings together the borrowing rules, object model, collection-based service, Swing interface, and SQLite storage developed in TSA1 through TSA4.

## Java concepts

- Classes, inheritance, interfaces, records, and enums
- Collections, generics, searching, and sorting
- Custom exceptions and validation
- Swing (`JFrame`, five tabs, tables, and dialogs)
- `SwingWorker` for database work outside the user interface thread
- JDBC prepared statements and transactions

## Features

The Catalog tab adds and searches books. Members registers and finds borrowers. Circulation issues and returns loans. Reservations adds, cancels, and manually processes the next available request. Reports shows counts, estimated active late fines, and sorted loan history. A book represents one physical copy per ISBN.

Student loans allow at most 14 days with a PHP 5 daily late fine. Faculty loans allow 30 days with PHP 2 per day. Guest loans allow 7 days with PHP 10 per day. There is no fixed limit on how many different books a member may borrow at once.

## Setup and run

Use JDK 17 or newer. Run commands from `Final-Project/BookBridge_Final`.

1. Download the SQLite JDBC dependency with Maven:

   ```sh
   mvn dependency:copy-dependencies -DoutputDirectory=lib
   ```

   If Maven is unavailable, download the pinned driver directly:

   ```sh
   mkdir -p lib
   curl -fL -o lib/sqlite-jdbc-3.50.3.0.jar https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.50.3.0/sqlite-jdbc-3.50.3.0.jar
   ```

2. Compile and launch:

   ```sh
   mkdir -p build/classes
   javac --release 17 -cp "lib/*" -d build/classes src/bookbridge/*/*.java
   java -cp "build/classes:lib/*" bookbridge.app.BookBridgeApp
   ```

   On Windows, use `;` instead of `:` in the Java classpath. Run from the project folder so the application can read `schema.sql`. The database is created at `data/bookbridge.db`. You may supply a different database path as the first application argument. Sample members and books are inserted only when a new database is initialized.

   On JDK 25, adding `--enable-native-access=ALL-UNNAMED` to `java` suppresses its native-access warning for the SQLite driver.

## Tests

```sh
javac --release 17 -cp "build/classes:lib/*" -d build/classes tests/bookbridge/tests/*.java
java -cp "build/classes:lib/*" bookbridge.tests.PolymorphismTest
java -cp "build/classes:lib/*" bookbridge.tests.LibraryServiceTest
java -cp "build/classes:lib/*" bookbridge.tests.PersistenceTest
java -Djava.awt.headless=true -cp "build/classes:lib/*" bookbridge.tests.SwingWorkerTest
```

The persistence tests create separate temporary SQLite databases. TSA2 legacy regression tests can also be run without changing TSA2:

```sh
mkdir -p build/legacy
javac --release 17 -d build/legacy ../../TSA2-Technical/BookBridge_TSA2/*.java tests/legacy/*.java
java -cp build/legacy PolymorphismTest
java -cp build/legacy LegacyRegressionTest
```

## Troubleshooting

- If Java cannot find the SQLite driver, repeat the dependency download and confirm that a JAR file is in `lib/`.
- If `schema.sql` cannot be found, run the commands from the `BookBridge_Final` folder.
- If the database is busy, close other BookBridge windows and try Refresh again. An existing database is never reset by the setup instructions.

Generated `build`, `target`, `lib`, `data`, and `submission` folders are excluded from Git. The source code, schema, Maven dependency file, and tests are included in this project.
