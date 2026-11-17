#!/bin/bash
# Local pre-CI gate for Blocker v3 (سد از صفر) — repo: rayganiali-creator/blocker-sadd
#  1. compiler-trap check: `return` inside an expression-body function
#     (kotlinc typecheck محلی این خطا را نمی‌گیرد ولی compileDebugKotlin می‌گیرد)
#  2. compile pure-Kotlin core + JUnit tests with local kotlinc, run them.
# Exits non-zero on any failure. Outputs under verify-out/.
# توجه: سبز شدن این اسکریپت «موفقیت بیلد» نیست؛ نتیجه CI بعداً باید دیده شود.
# Compose UI با این گیت چک نمی‌شود (نیازمند AARهای Compose) — آن را Gradle/CI می‌سنجد.
set -e
cd ~/workspace/blocker-v3
TB=~/workspace/tmp-build
export PATH="$TB/jdk-17.0.21+9/bin:$PATH"
OUT=~/workspace/blocker-v3/verify-out
KOTLINC=$TB/kotlinc/bin/kotlinc
JAVA=$TB/jdk-17.0.21+9/bin/java

echo "== 1. expression-body return trap =="
python3 tools/check_expr_return.py app/src/main/java app/src/test/java
echo "TRAP CHECK: 0 hits"

echo "== 2. compile core + tests =="
rm -rf $OUT && mkdir -p $OUT/test-classes
$KOTLINC \
  app/src/main/java/com/sadd/blocker/core/PersianNormalizer.kt \
  app/src/main/java/com/sadd/blocker/core/KeywordMatcher.kt \
  app/src/main/java/com/sadd/blocker/core/IntentDetector.kt \
  app/src/main/java/com/sadd/blocker/core/SessionCalculator.kt \
  app/src/test/java/com/sadd/blocker/core/PersianNormalizerTest.kt \
  app/src/test/java/com/sadd/blocker/core/KeywordMatcherTest.kt \
  app/src/test/java/com/sadd/blocker/core/IntentDetectorTest.kt \
  app/src/test/java/com/sadd/blocker/core/SessionCalculatorTest.kt \
  -cp "$TB/junit.jar:$TB/hamcrest.jar" -d $OUT/test-classes

echo "== 3. run JUnit =="
$JAVA -cp "$OUT/test-classes:$TB/junit.jar:$TB/hamcrest.jar:$TB/kotlinc/lib/kotlin-stdlib.jar" \
  org.junit.runner.JUnitCore \
  com.sadd.blocker.core.PersianNormalizerTest \
  com.sadd.blocker.core.KeywordMatcherTest \
  com.sadd.blocker.core.IntentDetectorTest \
  com.sadd.blocker.core.SessionCalculatorTest
echo "ALL CHECKS PASSED"
