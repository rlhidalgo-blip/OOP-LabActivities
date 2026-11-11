# TSA3 Test Report

Executed on 2026-11-11 (Asia/Manila client date) using Eclipse Temurin JDK 25.0.5. All Java sources were compiled with `--release 17 -Xlint:all -Werror`; no compiler warnings/errors occurred. Assertions execute regardless of `-ea`. The simplified build script deletes compiled classes before reusing build/classes and build/legacy, so stale classes cannot affect results.

## Observed results

- TSA3 service suite: **26 passed, 0 failed**.
- Packaged polymorphism regression: **3 passed, 0 failed**.
- Regression harness against unchanged TSA2 classes: **4 passed, 0 failed**.
- Total assertion-based scenarios: **33 passed, 0 failed**.
- Original TSA2 PolymorphismTest: executed successfully; print-only, not counted as additional assertion tests.
- Console demonstration: executed successfully with a fixed date and default UUID identifiers (IDs vary per run).
- SHA-256 preservation checks: all 12 original TSA1/TSA2 Java files matched.

## Reproduction and evidence

Run `sh build.sh` from the TSA3 project root, or follow the individual commands in [README](../README.md). Current execution output is [verification-final.txt](evidence/verification-final.txt); installed tool versions are [environment.txt](evidence/environment.txt). The earlier [verification.txt](evidence/verification.txt) is preserved unchanged as historical evidence. [Simplification checks](evidence/simplification-checks.txt) verify all 26 scenario bodies/assertions were retained, all original evidence hashes still match, and TSA1/TSA2 sources are unchanged.

Service assertions are in [LibraryServiceTest.java](../tests/bookbridge/tests/LibraryServiceTest.java). Each of the 26 scenarios now has a named test method; main only selects/runs scenarios and prints the final count. The extracted method bodies preserve every original setup step and assertion. Each row maps to the exact `PASS Txx` marker in the captured output. “PASS” records actual execution, not predicted behavior. Every service scenario also runs the `consistent` helper to check active/history/book/set agreement and reservation identity validity.

| Test ID | Purpose | Input / setup | Expected result (asserted) | Actual observed result | Status / evidence |
|---|---|---|---|---|---|
| T01 | Register members and books | Three member subtypes; four AVAILABLE books; whitespace lookup | 3 members, 4 books; normalized lookup returns original identity | All listed assertions completed; `PASS T01 registration and book addition` | PASS; [execution log](evidence/verification-simplified.txt) |
| T02 | Reject duplicate identities | Re-register S and A with surrounding whitespace | DuplicateEntityException twice; original identity and summary unchanged | All listed assertions completed; `PASS T02 duplicate member and ISBN rejection` | PASS; [execution log](evidence/verification-simplified.txt) |
| T03 | Issue a dated boundary loan | Student borrows A for 14 days on Nov 1 | ACTIVE; due Nov 15; loan lookup succeeds; one checked-out ISBN | All listed assertions completed; `PASS T03 successful issuance and due date` | PASS; [execution log](evidence/verification-simplified.txt) |
| T04 | Reject unavailable book | Student holds A; Faculty requests A | ItemUnavailableException; no additional loan or report change | All listed assertions completed; `PASS T04 unavailable book rejection` | PASS; [execution log](evidence/verification-simplified.txt) |
| T05 | Enforce all membership durations | Student 15, Faculty 31, Guest 8 days | LoanLimitExceededException for each; no history entries; all 4 books available | All listed assertions completed; `PASS T05 membership duration violation` | PASS; [execution log](evidence/verification-simplified.txt) |
| T06 | Return and freeze fine | Student 1-day loan on Nov 1; return Nov 5; repeat return | RETURNED; PHP 15 fixed fine; book available; repeat return rejected | All listed assertions completed; `PASS T06 successful and duplicate return with final fine` | PASS; [execution log](evidence/verification-simplified.txt) |
| T07 | Handle unfulfillable queues | Empty queue, then one reservation on checked-out A | Optional.empty in both cases; unavailable reservation stays queued | All listed assertions completed; `PASS T07 empty and wholly unavailable queues` | PASS; [execution log](evidence/verification-simplified.txt) |
| T08 | Handle missing entities | Unknown member, ISBN, loan; issue/return/reserve using missing entities | Lookup returns empty; operations throw NoSuchElementException without mutations | All listed assertions completed; `PASS T08 missing searches and operation entities` | PASS; [execution log](evidence/verification-simplified.txt) |
| T09 | Sort by three keys including ties | IDs 30/20/10/40; returned Nov 2 due date; two overdue Nov 3 ties; one due today Nov 5 | Order L-10, L-20, returned L-30, L-40; due today is not overdue; history unchanged | All listed assertions completed; `PASS T09 overdue due-date and ID sorting ties` | PASS; [execution log](evidence/verification-simplified.txt) |
| T10 | Compute aggregates and money | S/F/G loans for 1/2/3 days from Nov 1; report Nov 5 | 1 available, 3 checked out, one per type, PHP 29; after S return active fine PHP 14 | All listed assertions completed; `PASS T10 aggregate counts groups and fine totals` | PASS; [execution log](evidence/verification-simplified.txt) |
| T11 | Preserve per-book FIFO without global blocking | Queue F:A, G:A, G:B; return B first, then A | G receives B first; A queue unchanged; F then G receive A; direct bypass rejected | All listed assertions completed; `PASS T11 per-book FIFO and independent book progress` | PASS; [execution log](evidence/verification-simplified.txt) |
| T12 | Preserve every collection after failure | Snapshot all six required collections; invalid zero-day and over-limit requests | Both rejected; all six snapshots equal current values; invariants hold | All listed assertions completed; `PASS T12 failed issue leaves every collection unchanged` | PASS; [execution log](evidence/verification-simplified.txt) |
| T13 | Protect collections and domain values | Clear attempts on lists/set/maps; hold old values across issue/return | UnsupportedOperationException for all attempted mutations; old snapshots unchanged | All listed assertions completed; `PASS T13 immutable collection and domain snapshots` | PASS; [execution log](evidence/verification-simplified.txt) |
| T14 | Search and filter accurately | Mixed-case Java keyword; one checked-out book; Guest filter | 2 title matches, 1 checked-out and 3 available; Guest G; result lists immutable | All listed assertions completed; `PASS T14 title searches and status membership filters` | PASS; [execution log](evidence/verification-simplified.txt) |
| T15 | Cancel one entry without reordering | Three entries; cancel middle; repeat cancellation | First and last remain in order; cancelled value returned; missing cancellation rejected | All listed assertions completed; `PASS T15 reservation cancellation preserves remaining order` | PASS; [execution log](evidence/verification-simplified.txt) |
| T16 | Reject invalid reservations | Available unqueued book, own-held book, duplicate, excessive/negative duration | Appropriate exceptions; only original valid reservation remains | All listed assertions completed; `PASS T16 duplicate and invalid reservations` | PASS; [execution log](evidence/verification-simplified.txt) |
| T17 | Correct classification precedence | Valid available, valid unavailable, over-limit unavailable, zero days | APPROVED, RESERVED, DENIED; zero rejected; preview leaves summary unchanged | All listed assertions completed; `PASS T17 classification denial precedence and no mutation` | PASS; [execution log](evidence/verification-simplified.txt) |
| T18 | Preserve queue after processing ID conflict | Constant ID supplier; return L-same while R-same waits | Reused loan ID rejected; queue, books, history, and summary unchanged | All listed assertions completed; `PASS T18 duplicate generated ID leaves queue and books intact` | PASS; [execution log](evidence/verification-simplified.txt) |
| T19 | Reject impossible return dates atomically | Issue Nov 1; move clock back to Oct 31; return | IllegalArgumentException; original active/history values preserved | All listed assertions completed; `PASS T19 return with backward clock is atomic` | PASS; [execution log](evidence/verification-simplified.txt) |
| T20 | Reject invalid input safely | Blank/null inputs, null enum/object parameters, imported CHECKED_OUT book | Argument/null exceptions as specified; book count remains 4; state consistent | All listed assertions completed; `PASS T20 invalid inputs and imported checkout rejection` | PASS; [execution log](evidence/verification-simplified.txt) |
| T21 | Avoid invented count cap | Guest borrows all four different books for 7 days | 4 simultaneous valid loans accepted; checked-out set and history consistent | All listed assertions completed; `PASS T21 no invented simultaneous loan count limit` | PASS; [execution log](evidence/verification-simplified.txt) |
| T22 | Propagate external creation failures | ID supplier throws IllegalStateException before issuance | Same exception type propagates; circulation summary and invariants unchanged | All listed assertions completed; `PASS T22 supplier failure is propagated without state change` | PASS; [execution log](evidence/verification-simplified.txt) |
| T23 | Prevent cancelled ID reuse | Constant ID source; cancel R-same then reserve again | DuplicateEntityException; cancelled ID not reused; queue remains empty | All listed assertions completed; `PASS T23 cancelled reservation IDs cannot be reused` | PASS; [execution log](evidence/verification-simplified.txt) |
| T24 | Report empty state | New service with no entities or circulation | Zero counts/fines; empty sorted list; all three membership groups equal zero | All listed assertions completed; `PASS T24 empty reports include zero membership groups` | PASS; [execution log](evidence/verification-simplified.txt) |
| T25 | Reject stale return after reissue | Return old Student loan; Guest reborrows same ISBN; return old loan again | NoSuchElementException; new loan stays active; history retains 2 records | All listed assertions completed; `PASS T25 stale return cannot release a reissued book` | PASS; [execution log](evidence/verification-simplified.txt) |
| T26 | Retain fairness for available queued books | F waits for A; A returns; G joins queue; cancel F | Queue accepts G behind F; preview RESERVED; after cancellation G is served | All listed assertions completed; `PASS T26 available reserved book retains FIFO after cancellation` | PASS; [execution log](evidence/verification-simplified.txt) |

## Policy regression evidence

[PolymorphismTest.java](../tests/bookbridge/tests/PolymorphismTest.java) adapts the original Member-reference test with executable assertions.

| Test ID | Setup and purpose | Expected | Actual observed | Status |
|---|---|---|---|---|
| POLY-1 | Student through Member reference; boundary and unavailable requests; 3 late days | 14 days; PHP 5/day; PHP 15 fine; exact boundary approved; above denied; unavailable reserved | All policy/outcome/fine assertions passed | PASS |
| POLY-2 | Faculty through Member reference; same cases | 30 days; PHP 2/day; PHP 6 fine; correct outcomes | All policy/outcome/fine assertions passed | PASS |
| POLY-3 | Guest through Member reference; same cases | 7 days; PHP 10/day; PHP 30 fine; correct outcomes | All policy/outcome/fine assertions passed | PASS |

[LegacyRegressionTest.java](../tests/legacy/LegacyRegressionTest.java) is compiled separately against the untouched TSA2 files.

| Test ID | Setup and purpose | Expected | Actual observed | Status |
|---|---|---|---|---|
| LEGACY-1 | Original StudentMember, available/unavailable books, 3 late days, return | 14, PHP 5; APPROVED/RESERVED/DENIED; PHP 15; returned book available | All original policy/classification/fine/return assertions passed | PASS |
| LEGACY-2 | Original FacultyMember, same cases | 30, PHP 2; correct outcomes; PHP 6; return available | All assertions passed | PASS |
| LEGACY-3 | Original GuestMember, same cases | 7, PHP 10; correct outcomes; PHP 30; return available | All assertions passed | PASS |
| LEGACY-4 | Original Student 15-day unavailable request | RESERVED in original TSA2, capturing its precedence defect | RESERVED observed; intentional TSA3 DENIED correction verified by T17 | PASS |

Original print-only test output is retained in the same execution log (14/30/7 and PHP 5/2/10). No additional pre-existing assertion-based regression suite was present.

## Demonstration observations

The console demo approved a 14-day Student boundary request, issued a UUID-identified loan due 2026-11-25, produced a RESERVED preview and UUID-identified reservation, safely handled an unfulfillable queue, rejected Guest 8 days, returned the first loan, and fulfilled the Faculty reservation as a new UUID-identified loan. Final counts were 1 available, 1 checked out, 1 active, 2 historical, 0 pending, and PHP 0.00 active estimated fines.

## Exception evidence

Six supplementary captures executed against the same clean-build service classes and recorded the actual exception types/messages below. Each capture also compared all six collection snapshots before and after rejection. These captures illustrate the existing tests; the main suite count remains 33. The capture source and reproduction command are retained in `evidence/exception-evidence.txt`.

```text
E01 / T02 duplicate member
  Actual: DuplicateEntityException: Member ID already registered: S
  Expected type matched; all six collection snapshots unchanged: true
E02 / T02 duplicate ISBN
  Actual: DuplicateEntityException: ISBN already registered: A
  Expected type matched; all six collection snapshots unchanged: true
E03 / T05 duration limit
  Actual: LoanLimitExceededException: Requested 15 days; STUDENT allows at most 14.
  Expected type matched; all six collection snapshots unchanged: true
E04 / T04 unavailable book
  Actual: ItemUnavailableException: Book is already checked out: A
  Expected type matched; all six collection snapshots unchanged: true
E05 / T16 duplicate reservation
  Actual: DuplicateEntityException: Member already has a pending reservation for ISBN A
  Expected type matched; all six collection snapshots unchanged: true
E06 / T08 invalid return
  Actual: NoSuchElementException: Active loan not found: missing
  Expected type matched; all six collection snapshots unchanged: true
Exception captures: 6 verified; all collection snapshots unchanged after rejection.
```

## AI assistance disclosure

OpenAI Codex assisted with implementation, review, test execution, and submission documentation. The student should review and understand the submitted code. No manual user verification or instructor acceptance is claimed. See `AI_ASSISTANCE_DISCLOSURE.md` for the full disclosure.

## Verification limits

No screenshots or manual user acceptance tests were produced. The final PDF exports and architecture diagram were rendered and reviewed for readable layout; the SVG source also passed XML validation. Java 17 output compatibility was enforced by the compiler, but tests ran on JDK 25 rather than a separate Java 17 runtime. No instructor PDF was available for comparison. Concurrency, persistence, GUI integration, and runtime-failure recovery are outside TSA3 scope.
