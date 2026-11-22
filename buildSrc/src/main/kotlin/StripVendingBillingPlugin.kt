import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.artifacts.type.ArtifactTypeDefinition

/**
 * ثبت transform حذف کلاس‌های تکراری پرداخت (`StripVendingBillingClasses`)
 * فقط روی AAR کتابخانه مایکت (ir.myket:billing).
 *
 * چرا این ثبت داخل یک پلاگین buildSrc است و نه داخل اسکریپت
 * `app/build.gradle.kts`: دو راند تلاش برای نوشتن همین wiring در اسکریپت
 * Kotlin DSL در CI با خطای کامپایل اسکریپت شکست خورد (ران‌های ۲۰۶ و ۲۰۷ —
 * `Unresolved reference: it` و خطاهای استنتاج نوع، چون APIهای DSL
 * لامبدای گیرنده‌دار (receiver) می‌گیرند نه پارامتر `it`). اینجا همه‌چیز
 * با API خام Gradle و Actionهای صریح در کد Kotlin معمولی نوشته شده تا
 * هیچ جادوی DSLای در کار نباشد؛ سمت اسکریپت فقط یک خط
 * `apply<StripVendingBillingPlugin>()` است (اعمال با کلاس، بدون نیاز به
 * ثبت plugin id — کلاس‌های buildSrc برای اسکریپت قابل‌مشاهده‌اند).
 *
 * منطق attribute (جهتش در راند قبل با بایت‌کد Gradle راستی‌آزمایی شد):
 * - همه AARها پیش‌فرض «stripped» (true) حساب می‌شوند؛ فقط مایکت false
 *   می‌شود تا transform رویش اجرا شود. جهت برعکسِ این (نوع=false و
 *   مایکت=true) هرگز کار نمی‌کرد: آرتیفکت مایکت از اول true می‌گرفت و
 *   هیچ‌وقت با from=false transform جور نمی‌شد.
 * - transformهای Gradle «درخواست‌محور» هستند: فقط وقتی اجرا می‌شوند که
 *   مقدار هدف روی کانفیگ قابل‌حل درخواست شده باشد. با درخواست
 *   vending-stripped=true روی همه کانفیگ‌های قابل‌حل، تنها AAR مایکت
 *   (تنها false) بازنویسی می‌شود و بقیه AARها (true) دست‌نخورده می‌مانند.
 *   روی variant selection اثری ندارد چون این attribute روی variantهای
 *   تولیدکننده وجود ندارد (غایب = سازگار).
 *
 * دلیل امن‌بودن حذف از نسخه مایکت در
 * `StripVendingBillingClasses.kt` مستند شده (نسخه Poolakey ابرمجموعه
 * نسخه مایکت است؛ با javap روی بایت‌کد هر دو AAR راستی‌آزمایی شد).
 */
class StripVendingBillingPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val vendingStripped =
            Attribute.of("vending-stripped", Boolean::class.javaObjectType)
        val artifactType = ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
        val dependencies = project.dependencies

        dependencies.attributesSchema(Action { schema ->
            schema.attribute(vendingStripped)
        })

        dependencies.artifactTypes.maybeCreate("aar")
            .attributes.attribute(vendingStripped, true)

        dependencies.registerTransform(
            StripVendingBillingClasses::class.java,
            Action { spec ->
                spec.from.attribute(vendingStripped, false)
                    .attribute(artifactType, "aar")
                spec.to.attribute(vendingStripped, true)
                    .attribute(artifactType, "aar")
            }
        )

        dependencies.components.withModule("ir.myket:billing", Action { details ->
            details.allVariants(Action { variant ->
                variant.attributes.attribute(vendingStripped, false)
            })
        })

        project.configurations.configureEach(Action { config ->
            if (config.isCanBeResolved) {
                config.attributes.attribute(vendingStripped, true)
            }
        })
    }
}
