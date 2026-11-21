#!/bin/bash
# Local pre-CI gate for Blocker v3 (سد از صفر) — repo: rayganiali-creator/blocker-sadd
#  1. compiler-trap check: `return` inside an expression-body function
#     (kotlinc typecheck محلی این خطا را نمی‌گیرد ولی compileDebugKotlin می‌گیرد)
#  2. compile pure-Kotlin core + domain + JUnit tests with local kotlinc, run them.
# Exits non-zero on any failure. Outputs under verify-out/.
# توجه: سبز شدن این اسکریپت «موفقیت بیلد» نیست؛ نتیجه CI بعداً باید دیده شود.
# Compose UI و Room و Hilt با این گیت چک نمی‌شوند (نیازمند AARهای Compose/Room)
# — آن‌ها را Gradle/CI می‌سنجد. فایل‌های اندرویدی فقط با trap check بررسی می‌شوند.
set -e
cd ~/workspace/blocker-v3
TB=~/workspace/tmp-build
export PATH="$TB/jdk-17.0.21+9/bin:$PATH"
OUT=~/workspace/blocker-v3/verify-out
KOTLINC=$TB/kotlinc/bin/kotlinc
JAVA=$TB/jdk-17.0.21+9/bin/java
COROUTINES_JAR="$TB/aars/kotlinx-coroutines-core.jar"

echo "== 1. expression-body return trap =="
python3 tools/check_expr_return.py app/src/main/java app/src/test/java
echo "TRAP CHECK: 0 hits"

echo "== 2. compile pure Kotlin + tests =="
rm -rf $OUT && mkdir -p $OUT/test-classes
$KOTLINC \
  app/src/main/java/com/sadd/blocker/core/PersianNormalizer.kt \
  app/src/main/java/com/sadd/blocker/core/KeywordMatcher.kt \
  app/src/main/java/com/sadd/blocker/core/IntentDetector.kt \
  app/src/main/java/com/sadd/blocker/core/SectionDetector.kt \
  app/src/main/java/com/sadd/blocker/core/ProtectionGuardDetector.kt \
  app/src/main/java/com/sadd/blocker/core/TextRedactor.kt \
  app/src/main/java/com/sadd/blocker/core/KeyboardBlocker.kt \
  app/src/main/java/com/sadd/blocker/core/BlockThrottle.kt \
  app/src/main/java/com/sadd/blocker/core/SessionCalculator.kt \
  app/src/main/java/com/sadd/blocker/core/PinCrypto.kt \
  app/src/main/java/com/sadd/blocker/core/games/BlocksEngine.kt \
  app/src/main/java/com/sadd/blocker/core/games/EchoEngine.kt \
  app/src/main/java/com/sadd/blocker/core/games/PopEngine.kt \
  app/src/main/java/com/sadd/blocker/core/games/BreatheEngine.kt \
  app/src/main/java/com/sadd/blocker/core/games/FollowEngine.kt \
  app/src/main/java/com/sadd/blocker/core/games/LetItPassEngine.kt \
  app/src/main/java/com/sadd/blocker/domain/TimeFormat.kt \
  app/src/main/java/com/sadd/blocker/domain/StrictUnlock.kt \
  app/src/main/java/com/sadd/blocker/domain/StrictPassages.kt \
  app/src/main/java/com/sadd/blocker/domain/OnboardingSteps.kt \
  app/src/main/java/com/sadd/blocker/domain/TutorialPages.kt \
  app/src/main/java/com/sadd/blocker/domain/UninstallProtectionGate.kt \
  app/src/main/java/com/sadd/blocker/domain/JalaliDate.kt \
  app/src/main/java/com/sadd/blocker/domain/RuleValidation.kt \
  app/src/main/java/com/sadd/blocker/domain/BackupCodec.kt \
  app/src/main/java/com/sadd/blocker/domain/model/Models.kt \
  app/src/main/java/com/sadd/blocker/domain/model/SectionRule.kt \
  app/src/main/java/com/sadd/blocker/domain/repository/RuleRepositories.kt \
  app/src/main/java/com/sadd/blocker/domain/repository/SectionRepositories.kt \
  app/src/main/java/com/sadd/blocker/domain/usecase/RuleUseCases.kt \
  app/src/main/java/com/sadd/blocker/domain/usecase/RuleGuard.kt \
  app/src/main/java/com/sadd/blocker/domain/usecase/SessionUseCases.kt \
  app/src/main/java/com/sadd/blocker/domain/usecase/BackupUseCases.kt \
  app/src/test/java/com/sadd/blocker/core/PersianNormalizerTest.kt \
  app/src/test/java/com/sadd/blocker/core/KeywordMatcherTest.kt \
  app/src/test/java/com/sadd/blocker/core/IntentDetectorTest.kt \
  app/src/test/java/com/sadd/blocker/core/SectionDetectorTest.kt \
  app/src/test/java/com/sadd/blocker/core/ProtectionGuardDetectorTest.kt \
  app/src/test/java/com/sadd/blocker/core/TextRedactorTest.kt \
  app/src/test/java/com/sadd/blocker/core/KeyboardBlockerTest.kt \
  app/src/test/java/com/sadd/blocker/core/BlockThrottleTest.kt \
  app/src/test/java/com/sadd/blocker/core/SessionCalculatorTest.kt \
  app/src/test/java/com/sadd/blocker/core/PinCryptoTest.kt \
  app/src/test/java/com/sadd/blocker/core/games/BlocksEngineTest.kt \
  app/src/test/java/com/sadd/blocker/core/games/EchoEngineTest.kt \
  app/src/test/java/com/sadd/blocker/core/games/PopEngineTest.kt \
  app/src/test/java/com/sadd/blocker/core/games/BreatheEngineTest.kt \
  app/src/test/java/com/sadd/blocker/core/games/FollowEngineTest.kt \
  app/src/test/java/com/sadd/blocker/core/games/LetItPassEngineTest.kt \
  app/src/test/java/com/sadd/blocker/domain/RuleValidationTest.kt \
  app/src/test/java/com/sadd/blocker/domain/StrictUnlockTest.kt \
  app/src/test/java/com/sadd/blocker/domain/StrictSessionGuardTest.kt \
  app/src/test/java/com/sadd/blocker/domain/OnboardingStepsTest.kt \
  app/src/test/java/com/sadd/blocker/domain/TutorialPagesTest.kt \
  app/src/test/java/com/sadd/blocker/domain/UninstallProtectionGateTest.kt \
  app/src/test/java/com/sadd/blocker/domain/JalaliDateTest.kt \
  app/src/test/java/com/sadd/blocker/domain/BackupCodecTest.kt \
  app/src/test/java/com/sadd/blocker/data/FakeRepositories.kt \
  app/src/test/java/com/sadd/blocker/data/RepositoryTest.kt \
  app/src/test/java/com/sadd/blocker/domain/UseCaseTest.kt \
  -cp "$TB/junit.jar:$TB/hamcrest.jar:$COROUTINES_JAR" -d $OUT/test-classes

echo "== 3. run JUnit =="
$JAVA -cp "$OUT/test-classes:$TB/junit.jar:$TB/hamcrest.jar:$COROUTINES_JAR:$TB/kotlinc/lib/kotlin-stdlib.jar" \
  org.junit.runner.JUnitCore \
  com.sadd.blocker.core.PersianNormalizerTest \
  com.sadd.blocker.core.KeywordMatcherTest \
  com.sadd.blocker.core.IntentDetectorTest \
  com.sadd.blocker.core.SectionDetectorTest \
  com.sadd.blocker.core.ProtectionGuardDetectorTest \
  com.sadd.blocker.core.TextRedactorTest \
  com.sadd.blocker.core.KeyboardBlockerTest \
  com.sadd.blocker.core.BlockThrottleTest \
  com.sadd.blocker.core.SessionCalculatorTest \
  com.sadd.blocker.core.PinCryptoTest \
  com.sadd.blocker.core.games.BlocksEngineTest \
  com.sadd.blocker.core.games.EchoEngineTest \
  com.sadd.blocker.core.games.PopEngineTest \
  com.sadd.blocker.core.games.BreatheEngineTest \
  com.sadd.blocker.core.games.FollowEngineTest \
  com.sadd.blocker.core.games.LetItPassEngineTest \
  com.sadd.blocker.domain.RuleValidationTest \
  com.sadd.blocker.domain.StrictUnlockTest \
  com.sadd.blocker.domain.StrictSessionGuardTest \
  com.sadd.blocker.domain.OnboardingStepsTest \
  com.sadd.blocker.domain.TutorialPagesTest \
  com.sadd.blocker.domain.UninstallProtectionGateTest \
  com.sadd.blocker.domain.JalaliDateTest \
  com.sadd.blocker.domain.BackupCodecTest \
  com.sadd.blocker.data.RepositoryTest \
  com.sadd.blocker.domain.UseCaseTest
echo "ALL CHECKS PASSED"
