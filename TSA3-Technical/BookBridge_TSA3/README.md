# BookBridge TSA3

Collections-based library service extending the approved TSA2 object model. All new work is contained in this directory. TSA1 and TSA2 remain unchanged. No Swing, JDBC, database, external dependencies, or build framework is introduced.

## Requirements

JDK 17 or newer and a POSIX shell for `build.sh`. Verified on Eclipse Temurin 25.0.5 using `javac --release 17 -Xlint:all -Werror`. `--release 17` restricts syntax, APIs, and class output to Java 17. Execution was on JDK 25; a separate JDK 17 runtime was not available for runtime testing.

## Build, test, and demonstrate

Obtain the full repository after TSA3 has been published to GitHub:

```sh
git clone https://github.com/rlhidalgo-blip/OOP-LabActivities.git
cd OOP-LabActivities/TSA3-Technical/BookBridge_TSA3
```

From this project directory:

```sh
java -version
javac -version
sh build.sh
```

Or from the repository root:

```sh
sh TSA3-Technical/BookBridge_TSA3/build.sh
```

The script clears compiled classes in the ignored `build/classes` and `build/legacy` directories before each build, compiles with all warnings treated as errors, runs the assertion-based polymorphism and service suites, runs the fixed-date console demo, and separately compiles/runs the original TSA2 polymorphism test plus a new regression harness against the unchanged TSA2 classes. A failed compilation/assertion exits nonzero. Assertions run without requiring `-ea`. The full script needs the original `../../TSA2-Technical/BookBridge_TSA2/` sources for legacy regression checks; these already exist in this repository. Clone the complete repository so the legacy tests can find them.

For individual commands, from this directory:

```sh
mkdir -p build/classes build/legacy
find build/classes build/legacy -type f -name '*.class' -delete
find src tests/bookbridge -name '*.java' -print > build/sources.txt
javac --release 17 -Xlint:all -Werror -d build/classes @build/sources.txt
java -cp build/classes bookbridge.app.BookBridgeApp
java -cp build/classes bookbridge.tests.PolymorphismTest
java -cp build/classes bookbridge.tests.LibraryServiceTest
javac --release 17 -Xlint:all -Werror -d build/legacy ../../TSA2-Technical/BookBridge_TSA2/*.java tests/legacy/*.java
java -cp build/legacy PolymorphismTest
java -cp build/legacy LegacyRegressionTest
```

The demonstration uses 2026-11-11, so outcomes and dates are reproducible. Its UUID loan/reservation IDs vary per run; tests supply predictable IDs when they need specific values. Production-style construction with `new LibraryService()` uses the local system clock and UUID IDs. Nothing is saved between processes.

## Structure

```text
src/bookbridge/
  app/          BookBridgeApp
  model/        Member hierarchy, LibraryBook, LoanRecord, Reservation,
                Notifiable, enums, CirculationSummary
  service/      LibraryService: all collection and business operations
  exception/    DuplicateEntityException, ItemUnavailableException,
                LoanLimitExceededException
  util/         Validation, generic Lookup
 tests/
  bookbridge/tests/  TSA3 assertions and adapted PolymorphismTest
  legacy/            assertions compiled against original TSA2 classes
 docs/               audit, strategy, test report, diagrams, rubric,
                     AI disclosure, and captured execution evidence
```

## Policies and operations

| Membership | Maximum duration | Daily late fine |
|---|---:|---:|
| Student | 14 days | PHP 5.00 |
| Faculty | 30 days | PHP 2.00 |
| Guest | 7 days | PHP 10.00 |

Member subclasses retain runtime polymorphism. There is **no simultaneous-loan-count cap**. `LoanLimitExceededException` means duration violation. Zero/negative days are invalid. Exceeding a member's limit is rejected before checking availability. Exactly the allowed maximum is accepted.

The service supports member registration/lookup/listing/type filters; book addition/ISBN lookup/title keyword search/status filters; issue/return/active lookup/history; reserve/cancel/process/list; overdue-first sorting and circulation reports. IDs and names are stripped of surrounding whitespace. IDs are case-sensitive, ISBN is treated as an opaque identifier (no checksum validation), and title searches are case-insensitive using `Locale.ROOT`. Blank search keywords are rejected. Missing valid lookup keys return `Optional.empty()`; missing entities required by operations throw `NoSuchElementException`.

New books must be AVAILABLE. The service changes book status only when issuing or returning a loan. Returned collections are immutable snapshots. Book, loan, and reservation records and the three supplied member subclasses are immutable. Previously obtained book/loan snapshots do not change when the service replaces its current values; query again for current status.

`classifyRequest` returns an enum preview (APPROVED/RESERVED/DENIED) without changing state. To actually reserve, call `reserveBook`; to borrow, call `issueLoan`. Denied requests and reservations are not loan history entries.

### Reservations

Only checked-out books or books with an existing queue can be reserved. A borrower cannot reserve their own currently held book, and a member cannot have duplicate pending reservations for an ISBN. Duration rules apply when reserving and processing. Cancellation removes only the selected reservation.

`processNextReservation()` scans global insertion order and issues one loan to the earliest fulfillable reservation. Unavailable entries stay in their original positions; later entries for other available books can proceed. Each ISBN retains FIFO order. If none can be fulfilled (including empty queues), it returns `Optional.empty()` without changes. Other processing failures propagate with the queue unchanged. Direct `issueLoan` cannot bypass a book's queue, even for its first waiting member: use queue processing. Returns release availability but do not automatically process reservations or print notifications.

### Dates, sorting, and fines

Due date is issue date plus requested days. A loan is overdue only when still active and its due date is before the clock's current date; due today is not overdue. `getSortedLoans()` returns **all history**, including returned entries, ordered by active overdue first, then due date ascending, then loan ID ascending. History itself remains in issuance order.

`getTotalEstimatedFines()` sums active loans only. The final charge for a returned loan is retained in its historical value and stops growing on the return date. Fines are whole late days multiplied by the member's daily rate, using `BigDecimal`. There is no payment ledger. Membership aggregates include all three types, including zero counts.

## Documentation and verified results

- [Complete file inventory](docs/FILE_INVENTORY.md)
- [TSA2 audit and adaptations](docs/TSA2_AUDIT.md)
- [Collection Strategy PDF](docs/BookBridge_Collection_Strategy.pdf) and [editable source](docs/COLLECTION_STRATEGY.md)
- [Test Report PDF](docs/TSA3_Test_Report.pdf) and [editable source](docs/TSA3_TEST_REPORT.md): 26 service scenarios, 3 adapted polymorphism scenarios, and 4 legacy regression scenarios passed.
- [Architecture PDF](docs/BookBridge_TSA3_Architecture.pdf), [explanation](docs/ARCHITECTURE.md), [editable Mermaid](docs/architecture.mmd), [exportable SVG](docs/architecture.svg)
- [Rubric assessment](docs/RUBRIC_CHECKLIST.md)
- [AI assistance disclosure](docs/AI_ASSISTANCE_DISCLOSURE.md)
- [Final clean build/test/demo output](docs/evidence/verification-final.txt)
- [Actual exception captures](docs/evidence/exception-evidence.txt)
- [Earlier archive verification, retained as historical evidence](docs/evidence/package-verification.txt)
- [Final preservation and document checks](docs/evidence/finalization-checks.txt)
- [Earlier simplification build output, preserved](docs/evidence/verification-simplified.txt)
- [Original execution evidence, preserved](docs/evidence/verification.txt)
- [Simplification and preservation checks](docs/evidence/simplification-checks.txt)
- [Source preservation checks](docs/evidence/source-preservation.txt)

## Canvas submission: three PDFs and the GitHub link

The professor does not accept ZIP files. Upload these three documents individually:

- [BookBridge_Collection_Strategy.pdf](docs/BookBridge_Collection_Strategy.pdf)
- [TSA3_Test_Report.pdf](docs/TSA3_Test_Report.pdf)
- [BookBridge_TSA3_Architecture.pdf](docs/BookBridge_TSA3_Architecture.pdf)

Paste this exact TSA3 project URL into the Canvas submission text:

https://github.com/rlhidalgo-blip/OOP-LabActivities/tree/main/TSA3-Technical/BookBridge_TSA3

Use the [Canvas submission checklist](docs/CANVAS_SUBMISSION_CHECKLIST.md). The complete project belongs in this existing GitHub repository: include src/, tests/, docs/, README.md, build.sh, and .gitignore. The README commands above compile, run, and test it from the full repository clone.

Publication status at preparation: TSA3 is ready locally but is still untracked. The folder URL must be checked after a separately authorized commit and push, before submitting it to the professor. No commit, push, or Canvas upload has been performed.

The previously generated submission archive is obsolete and must not be uploaded or used for submission. It remains ignored under submission/. Earlier archive verification logs are retained only as historical test evidence. Exclude build/, submission/, .class files, .DS_Store, and temporary exports from GitHub. Retain the three required PDF documents, editable documentation, AI disclosure, and execution evidence.

## Limitations and TSA4 preparation

The service is intentionally single-threaded, in-memory, and unsynchronized. Validation/construction precedes mutation; this protects normal business failures, not JVM termination/out-of-memory errors or concurrent access. Injected clocks/ID suppliers must be side-effect-free with respect to the service; reentrant mutations are unsupported. History and used identifiers grow for the lifetime of the service. Each ISBN represents one physical copy. No persistence, removals of members/books, GUI, notification transport, authentication, renewals, fine payments, or adjustable membership policies are implemented.

Stable string IDs, date handling, and a console-independent service support later TSA4 work. Future database updates will need transactions and a concurrency strategy before background tasks are added. No simultaneous-loan cap is assumed. Any future Member subclass must keep its identity and borrowing policies immutable, as the three supplied subclasses do.

The instructor PDF was not present in the repository or provided attachment. Compliance was assessed against the pasted task and final user instructions; exact PDF-specific presentation requirements remain unverified.
