import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * حذف کلاس‌های تکراری `com.android.vending.billing.IInAppBillingService*`
 * از داخل AAR کتابخانه پرداخت مایکت (ir.myket:billing).
 *
 * چرا لازم است: هر دو کتابخانه پرداخت (مایکت و Poolakey کافه‌بازار) استاب‌های
 * AIDL سرویس پرداخت گوگل‌پلی (`IInAppBillingService` + `$Default` + `$Stub` +
 * `$Stub$Proxy`) را داخل `classes.jar` خودشان جاسازی کرده‌اند و تسک
 * `:app:checkDebugDuplicateClasses` روی کلاس تکراری بیلد را رد می‌کند
 * (ران ۲۰۵). این کلاس‌ها از هیچ ماژول transitiveای نمی‌آیند (POM هر دو
 * کتابخانه چک شد) پس `exclude` معمولی جواب نمی‌دهد؛ تنها راه، بازنویسی
 * خود AAR است.
 *
 * چرا حذف از نسخه مایکت امن است (با javap روی بایت‌کد هر دو AAR راستی‌آزمایی شد):
 * - رابط Poolakey ابرمجموعه رابط مایکت است: همان ۷ متد مایکت
 *   (isBillingSupported … getPurchaseConfig) با کدهای تراکنش یکسان ۱ تا ۷،
 *   به‌علاوه ۳ متد اضافه (getBuyIntentV3، checkTrialSubscription،
 *   getFeatureConfig). پس نسخه باقی‌مانده Poolakey روی سیم (wire) دقیقاً
 *   همان فراخوانی‌هایی را می‌فرستد که کد مایکت انتظار دارد.
 * - هیچ کلاس مایکت بیرون از پکیج vending به فیلد `DESCRIPTOR` یا کلاس کمکی
 *   `$_Parcel` (که فقط در نسخه مایکت هست) ارجاع نمی‌دهد؛ تنها مصرف‌کننده
 *   رابط، `ServiceIAB` است و فقط همان ۷ متد مشترک را صدا می‌زند.
 * برعکسش (حذف از Poolakey) امن نبود: کد Poolakey ممکن است ۳ متد اضافه را
 * صدا بزند که در نسخه مایکت وجود ندارند.
 *
 * رفتار: AAR ورودی باز می‌شود، از `classes.jar` آن فقط ورودی‌های
 * `com/android/vending/billing/IInAppBillingService*` حذف می‌شوند و بقیه
 * (مانیفست، ریسورس‌ها، R.txt و …) دست‌نخورده دوباره zip می‌شود. اگر چنین
 * ورودی‌ای نباشد، خروجی معادل ورودی است.
 */
@CacheableTransform
abstract class StripVendingBillingClasses : TransformAction<TransformParameters.None> {

    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        val output = outputs.file(input.name)
        ZipInputStream(input.inputStream().buffered()).use { zin ->
            ZipOutputStream(output.outputStream().buffered()).use { zout ->
                var entry = zin.nextEntry
                while (entry != null) {
                    val bytes = zin.readBytes()
                    val newBytes = if (entry.name == "classes.jar") stripJar(bytes) else bytes
                    writeEntry(zout, entry.name, newBytes)
                    entry = zin.nextEntry
                }
            }
        }
    }

    private fun stripJar(jarBytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        ZipInputStream(jarBytes.inputStream()).use { zin ->
            ZipOutputStream(out).use { zout ->
                var entry = zin.nextEntry
                while (entry != null) {
                    val bytes = zin.readBytes()
                    if (!isVendingBillingClass(entry.name)) {
                        writeEntry(zout, entry.name, bytes)
                    }
                    entry = zin.nextEntry
                }
            }
        }
        return out.toByteArray()
    }

    private fun isVendingBillingClass(name: String): Boolean =
        name.startsWith("com/android/vending/billing/IInAppBillingService")

    private fun writeEntry(zout: ZipOutputStream, name: String, bytes: ByteArray) {
        val newEntry = ZipEntry(name)
        // زمان ثابت برای خروجی تکرارپذیر (مبدأ زمان zip: 1980-01-01)
        newEntry.time = FIXED_TIME
        zout.putNextEntry(newEntry)
        zout.write(bytes)
        zout.closeEntry()
    }

    private companion object {
        const val FIXED_TIME = 315532800000L
    }
}
