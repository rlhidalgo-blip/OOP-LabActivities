# TSA2 audit and reuse

Audit completed before implementation; initial plan approved by the user. All eleven Java files in `TSA2-Technical/BookBridge_TSA2/` were read. TSA1's source was also read to cross-check the approved rules. No AGENTS.md or instructor PDF was found in the repository.

## Baseline verification

Temurin `java` and `javac` report 25.0.5. The original eleven TSA2 files compiled with `--release 17 -Xlint:all -Werror` into `/tmp/bookbridge-tsa2-baseline`. The original PolymorphismTest executed and printed 14/30/7 days and PHP 5/2/10. No compiler diagnostics were produced. It is a print-only demonstration, not an assertion suite.

## Responsibilities and adaptations

| TSA2 source | Existing responsibility | TSA3 reuse/adaptation |
|---|---|---|
| BookBridgeApp | Scanner input, five parallel arrays, classification, object creation, output and totals | Same domain workflow demonstrated via service; input/output no longer owns business state |
| Member | Abstract ID/name object with maximum-day and daily-fine methods | Preserve abstraction, method names, and policies; final identity fields, ordinary abstract Member with immutable supplied subclasses, membership enum |
| StudentMember | Overrides 14 days/PHP 5 | Preserved |
| FacultyMember | Overrides 30 days/PHP 2 | Preserved |
| GuestMember | Overrides 7 days/PHP 10 | Preserved |
| LibraryBook | ISBN/title and mutable boolean availability | Immutable record, BookStatus, service-controlled replacement |
| LoanRecord | Static integer ID, references, requested/late days, return flag and fine | Immutable dated loan, stable ID supplied by service, LoanStatus derived from return date; ISBN reference avoids stale mutable book references |
| Reservation | Member/book references and printed notification | Immutable ID/member-ID/ISBN/duration value; queue lifecycle managed by service |
| Notifiable | Void method printing a reservation notification | Retained interface concept as notificationMessage(); returns text for app delivery with no service/model I/O |
| LibraryService | String request classifier only | Retain preview behavior with enum outcomes, add collection ownership and every requested service operation |
| PolymorphismTest | Prints subclass day/fine results through Member references | Original run unchanged; separate packaged assertion-based adaptation preserves intent and strengthens verification |

Original source is preserved; adapted classes live in TSA3 packages. This is an extension of TSA2's domain design rather than an unrelated application replacement.

## Defects and intentional corrections

- Availability is checked before duration in TSA1/TSA2, allowing an over-limit unavailable request to be RESERVED. TSA3 checks the limit first and returns DENIED/throws LoanLimitExceededException as appropriate. LEGACY-4 captures the original behavior, while T17 captures the required correction.
- Direct classification accepts nonpositive days; console validation alone is insufficient. TSA3 validates in the business layer.
- The app creates a LoanRecord for every outcome and totals fines even for denied/reserved requests. TSA3 records only issued loans and estimates fines only for active loans.
- Public book/loan mutations can change state independently, and repeated returns are not checked. TSA3 removes mutation methods and replaces immutable registered values through the service.
- Loan/member/book nulls, reservation duplicates, and entity registration are not consistently validated. TSA3 centralizes lookup and operation validation.
- No due dates or real reservation processing exist. TSA3 adds dates, injected Clock, a fair queue, cancellation, and immutable reports.
- The static counter resets and is not persistence-ready. TSA3 supplies IDs externally (UUID by default) and rejects reuse; deterministic tests inject sequences.
- No simultaneous-loan-count policy exists. None is added. LoanLimitExceededException enforces duration, and T21 verifies multiple simultaneous valid loans.

## Preservation evidence

Before edits, SHA-256 hashes of all twelve TSA1/TSA2 Java files were captured. Final hashes matched; see `evidence/source-preservation.txt`. Git status shows only the new TSA3 directory. No commits or pushes were performed.
