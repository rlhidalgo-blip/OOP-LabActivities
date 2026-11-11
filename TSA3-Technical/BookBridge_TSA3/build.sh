#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes build/legacy
# Remove only compiled classes so old output cannot affect a new build.
find build/classes build/legacy -type f -name '*.class' -delete
find src tests/bookbridge -name '*.java' -print > build/sources.txt
javac --release 17 -Xlint:all -Werror -d build/classes @build/sources.txt
printf 'TSA3 Java 17 compilation: PASS (all warnings treated as errors)\n'
java -cp build/classes bookbridge.tests.PolymorphismTest
java -cp build/classes bookbridge.tests.LibraryServiceTest
java -cp build/classes bookbridge.app.BookBridgeApp
# Original TSA2 classes stay separate because they use the default package.
find ../../TSA2-Technical/BookBridge_TSA2 tests/legacy -name '*.java' -print > build/legacy-sources.txt
javac --release 17 -Xlint:all -Werror -d build/legacy @build/legacy-sources.txt
printf 'TSA2 Java 17 compilation: PASS (all warnings treated as errors)\n'
java -cp build/legacy PolymorphismTest
java -cp build/legacy LegacyRegressionTest
printf 'Verification completed; compiled classes: build/classes and build/legacy\n'
