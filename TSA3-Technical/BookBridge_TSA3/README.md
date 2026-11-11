# BookBridge TSA3

BookBridge TSA3 is a Java console application for managing library loans and reservations. It extends the TSA2 project by using collections to store and manage library records.

## Java Concepts Demonstrated

- Inheritance, abstraction, and polymorphism
- Packages and enums
- Lists, maps, sets, and queues
- Generics
- Custom exceptions and input validation
- Records, lambda expressions, and comparators

## Main Features

- Register members and add books.
- Search for members and books.
- Issue and return loans using membership borrowing rules.
- Create, cancel, and process book reservations.
- Calculate late fines.
- Sort loan records and display circulation reports.

## Compiling and Running

Use JDK 17 or newer. Open a terminal in the repository folder and run:

```sh
cd TSA3-Technical/BookBridge_TSA3
mkdir -p build/classes
javac --release 17 -d build/classes src/bookbridge/*/*.java
java -cp build/classes bookbridge.app.BookBridgeApp
```

The application runs a sample demonstration of the library operations.

## Testing

From the same project folder, compile and run the TSA3 tests:

```sh
javac --release 17 -cp build/classes -d build/classes tests/bookbridge/tests/*.java
java -cp build/classes bookbridge.tests.PolymorphismTest
java -cp build/classes bookbridge.tests.LibraryServiceTest
```

To check the original TSA2 behavior, run:

```sh
mkdir -p build/legacy
javac --release 17 -d build/legacy ../../TSA2-Technical/BookBridge_TSA2/*.java tests/legacy/*.java
java -cp build/legacy PolymorphismTest
java -cp build/legacy LegacyRegressionTest
```

The tests check borrowing rules, collection consistency, searching, sorting, reports, and reservation ordering.
