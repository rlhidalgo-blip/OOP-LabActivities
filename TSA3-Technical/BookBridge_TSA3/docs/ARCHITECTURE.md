# BookBridge TSA3 architecture

[Open the submission PDF](BookBridge_TSA3_Architecture.pdf) or [exportable SVG](architecture.svg). It is a standalone vector image suitable for a browser, printing, or insertion into a document. [Mermaid source](architecture.mmd) is provided for editing. SVG syntax was parsed as XML; the PDF export was rendered and visually reviewed during final submission preparation.

Application depends on service and models. LibraryService owns every mutable collection and business operation, depends on model values and exceptions, and uses injected Clock/ID Supplier plus generic Lookup/Validation helpers. Models contain domain data, date/fine calculations, and the original polymorphic member policies. Records and the three supplied final member subclasses are immutable. They perform no console I/O. Exceptions communicate rejected business operations to the caller; the demo catches only its intentionally exercised duration exception. Tests call the service directly, without console, Swing, or JDBC dependencies.

## Exception behavior

| Type | Responsible use |
|---|---|
| DuplicateEntityException | Service rejects duplicate member IDs, ISBNs, pending member/book pairs, and reused generated identifiers |
| ItemUnavailableException | Service rejects an already borrowed book or an attempt to bypass pending reservations |
| LoanLimitExceededException | Service rejects a requested duration greater than the member's maximum; no simultaneous-count limit is implied |
| IllegalArgumentException | Invalid text/duration, backward return dates, own-held reservations, available unqueued reservations, or importing a checked-out book |
| NoSuchElementException | A required member/book/active loan/pending cancellation target does not exist |
| NullPointerException | A required object dependency or enum argument is null, via Objects.requireNonNull |
| IllegalStateException | Defensive detection of inconsistent private return state |

Exceptions carry descriptive messages. No exceptions are wrapped, so underlying ID-source/clock exceptions and their original causes propagate intact. There are no empty catches; test harness catches only to record failure/verify an expected exception.

## Why retain these Java files?

All 20 production files and three test files are retained. These are ordinary domain types, one service, and small shared helpers; no repository framework, dependency injection container, or DAO layer is introduced.

| File / type | Purpose |
|---|---|
| BookBridgeApp | Runs a console example using service methods |
| LibraryService | Owns collections and performs every business operation |
| Member | TSA2 abstract base for identity and polymorphic borrowing policies |
| StudentMember | Supplies the 14-day maximum and PHP 5 daily fine |
| FacultyMember | Supplies the 30-day maximum and PHP 2 daily fine |
| GuestMember | Supplies the 7-day maximum and PHP 10 daily fine |
| LibraryBook | Immutable ISBN/title/status value |
| LoanRecord | Immutable loan dates/return state plus late-fine calculation |
| Reservation | Immutable pending request identity, member ID, ISBN, and duration |
| Notifiable | Preserves TSA2 interface continuity; provides message text without printing |
| BookStatus | Required AVAILABLE/CHECKED_OUT enum |
| LoanStatus | Required ACTIVE/RETURNED enum, derived from return date |
| MembershipType | Required STUDENT/FACULTY/GUEST enum for filtering and reports |
| RequestOutcome | Small optional enum preserving APPROVED/RESERVED/DENIED previews safely |
| CirculationSummary | Small optional record giving the required report a typed result |
| DuplicateEntityException | Required duplicate member/ISBN/reservation/identifier errors |
| ItemUnavailableException | Required unavailable-book or queue-bypass errors |
| LoanLimitExceededException | Required duration-limit error; does not impose a loan-count cap |
| Lookup | Required useful generic abstraction: shared typed registry lookup |
| Validation | Shared blank-text checking and whitespace normalization |
| LibraryServiceTest | 26 named scenarios plus shared assertions and consistency checks |
| Packaged PolymorphismTest | Verifies inherited TSA2 policies through Member references |
| LegacyRegressionTest | Separately exercises the unchanged TSA2 classes |

The small MutableClock nested inside LibraryServiceTest is a test helper, not another production class. It advances dates without waiting. The standard Clock and Supplier constructor arguments let tests control dates and IDs; they require no framework or additional infrastructure.

## Simplified control flow

Member is an ordinary abstract class, matching TSA2's inheritance approach. The supplied subclasses keep fixed policies; future subclasses must preserve that immutability contract. Records, comparators, lambdas, and method references supply the required modern-Java features without needing sealed classes.

issueLoan and processNextReservation each check duration before their common createLoan helper. The helper constructs all replacement values before updating collections and checks book status plus the uniqueness set; it no longer repeats duration validation or scans active loans. Constructors still reject invalid standalone model values.

processNextReservation uses one Iterator and no temporary visited-ISBN set. It scans in insertion order, skips unavailable books, and returns immediately after issuing the first fulfillable entry. The first available entry for a book is necessarily its earliest waiting entry. hasPendingReservation uses a boolean loop because callers only need to know whether a queue exists.

LibraryServiceTest.main now lists named scenario methods; setups and assertions remain unchanged. build.sh clears only compiled classes and uses predictable ignored build/classes and build/legacy directories. Obsolete build/run.* compiler directories were removed; all original evidence files remain byte-for-byte unchanged. Current test output is in [verification-simplified.txt](evidence/verification-simplified.txt).

## Reading order and TSA4 boundary

Start with Member and its three subclasses, then LibraryBook/LoanRecord/Reservation. Read LibraryService's collection fields, issueLoan, returnBook, and processNextReservation, followed by search/report methods. Finally, run BookBridgeApp and inspect one named test at a time.

Stable IDs, explicit dates/statuses, and the UI-independent service support later TSA4 adaptation. Persistence and concurrency remain future work; see [README limitations](../README.md#limitations-and-tsa4-preparation).
