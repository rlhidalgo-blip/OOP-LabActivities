# TSA3 submission files

The TSA3 project contains 50 submission files, including all 20 production Java files and three test files. Java source and tests were preserved without changes during finalization. No TSA1/TSA2 files were modified.

## Canvas

Upload these three PDFs individually and paste the TSA3 GitHub folder URL into the submission text. ZIP files are not allowed.

- BookBridge_Collection_Strategy.pdf (3 pages)
- TSA3_Test_Report.pdf (8 pages; verified tests and exception evidence)
- BookBridge_TSA3_Architecture.pdf (1 vector page)

GitHub project URL:

https://github.com/rlhidalgo-blip/OOP-LabActivities/tree/main/TSA3-Technical/BookBridge_TSA3

See [CANVAS_SUBMISSION_CHECKLIST.md](CANVAS_SUBMISSION_CHECKLIST.md). The files are ready locally; a separately authorized commit and push are still required to publish TSA3 before the link is submitted. The full source, tests, README, build script, AI disclosure, and documentation remain in this existing repository.

## GitHub

Include src/, tests/, docs/ (PDFs, editable sources, and execution evidence), README.md, build.sh, and .gitignore. Exclude build/, submission/, .class files, .DS_Store, and temporary exports. Required PDF deliverables are intentionally retained. No commit or push has been performed.

## File types

| Files | Classification |
|---|---|
| src/**/*.java, tests/**/*.java | Authored, AI-assisted Java source; preserve all 23 files |
| build.sh and .gitignore | Build support |
| Markdown and architecture.mmd | Editable documentation |
| docs/*.pdf and architecture.svg | Submission documents and vector diagram |
| docs/evidence/* | Actual execution, exception, checksum, and preservation evidence |
| build/* | Generated compiler output, excluded from submission/Git |
| submission/*.zip | Obsolete archive; do not use or upload; remains excluded from Git |

## Complete TSA3 project inventory

```text
.gitignore
README.md
build.sh
docs/AI_ASSISTANCE_DISCLOSURE.md
docs/ARCHITECTURE.md
docs/BookBridge_Collection_Strategy.pdf
docs/BookBridge_TSA3_Architecture.pdf
docs/CANVAS_SUBMISSION_CHECKLIST.md
docs/COLLECTION_STRATEGY.md
docs/FILE_INVENTORY.md
docs/RUBRIC_CHECKLIST.md
docs/TSA2_AUDIT.md
docs/TSA3_TEST_REPORT.md
docs/TSA3_Test_Report.pdf
docs/architecture.mmd
docs/architecture.svg
docs/evidence/environment.txt
docs/evidence/exception-evidence.txt
docs/evidence/finalization-checks.txt
docs/evidence/original-source-sha256.txt
docs/evidence/package-verification.txt
docs/evidence/simplification-checks.txt
docs/evidence/source-preservation-final.txt
docs/evidence/source-preservation.txt
docs/evidence/verification-final.txt
docs/evidence/verification-simplified.txt
docs/evidence/verification.txt
src/bookbridge/app/BookBridgeApp.java
src/bookbridge/exception/DuplicateEntityException.java
src/bookbridge/exception/ItemUnavailableException.java
src/bookbridge/exception/LoanLimitExceededException.java
src/bookbridge/model/BookStatus.java
src/bookbridge/model/CirculationSummary.java
src/bookbridge/model/FacultyMember.java
src/bookbridge/model/GuestMember.java
src/bookbridge/model/LibraryBook.java
src/bookbridge/model/LoanRecord.java
src/bookbridge/model/LoanStatus.java
src/bookbridge/model/Member.java
src/bookbridge/model/MembershipType.java
src/bookbridge/model/Notifiable.java
src/bookbridge/model/RequestOutcome.java
src/bookbridge/model/Reservation.java
src/bookbridge/model/StudentMember.java
src/bookbridge/service/LibraryService.java
src/bookbridge/util/Lookup.java
src/bookbridge/util/Validation.java
tests/bookbridge/tests/LibraryServiceTest.java
tests/bookbridge/tests/PolymorphismTest.java
tests/legacy/LegacyRegressionTest.java
```
