# سد — Blocker v3 (از صفر)

ریپوی گیت‌هاب (تاییدشده): **`rayganiali-creator/blocker-sadd`** — ساخته شده و خالی است.

## وضعیت فاز ۱

- معماری: [`ARCHITECTURE.md`](ARCHITECTURE.md) (فارسی)
- موکاپ‌ها: `mockups/` — ۴ تصویر موبایل عمودی (داشبورد، قوانین، صفحه بلاک، سشن فعال)
- اسکلت: Kotlin + Compose + Material 3، پکیج `com.sadd.blocker`، minSdk 26 / targetSdk 35

## وضعیت فاز ۲ — UI کامل + Room + تنظیمات

- ناوبری کامل ۱۲ صفحه با نوار پایین (۵ مقصد اصلی) و تم روشن/تیره از DataStore.
- Room کامل: ۵ Entity با ایندکس یکتا (host و normalizedText) + همه DAOها + Repositoryهای Room.
- داشبورد واقعی: باقی‌مانده سشن از `endEpochMs`، شمارش قوانین فعال، سلامت واقعی مجوزها.
- مدیریت قوانین: اپ‌های نصب‌شده (PackageManager) با جستجو و انتخاب چندتایی، وب‌سایت با نرمال‌سازی دامنه، کلمات با نرمال‌ساز فارسی و whole-word.
- PIN با SHA-256 + salt (هرگز plaintext)؛ بکاپ/ریستور JSON نسخه‌دار با اعتبارسنجی.
- تست واحد: **۶۷ تست** (۳۰ تست فاز ۱ + ۳۷ تست جدید برای PinCrypto، BackupCodec، Validation، Repositoryهای جعلی و UseCaseها) — `bash verify.sh` سبز است.

## ساخت و تست محلی

```bash
bash verify.sh   # گیت محلی: تله return در expression-body + کامپایل core/domain + اجرای JUnit
./gradlew assembleDebug   # بیلد کامل (در CI هم اجرا می‌شود)
```

**توجه:** سبز شدن `verify.sh` موفقیت بیلد نیست؛ نتیجه CI باید جداگانه دیده شود.
Compose UI و Room و Hilt با `verify.sh` چک نمی‌شوند (نیازمند AARهای Compose/Room)
— آن‌ها را Gradle/CI می‌سنجد. فایل‌های اندرویدی فقط با trap check بررسی می‌شوند.
محدودیت این VM: ارتباط IPC دیمن Gradle خراب است (در Gradle 8.9 و 9.3.1 تست شد)،
پس بیلد کامل Gradle فقط در CI (GitHub Actions) قابل اجراست — گیت محلی همان روش پروژه قبلی است.

## اصل طلایی

خواندن مقاله یا پیام حاوی کلمه ممنوعه **هرگز** بلاک نمی‌شود.
فقط جستجوی عمدیِ سابمیت‌شده، ناوبری به دامنه ممنوعه، یا باز کردن اپ ممنوعه بلاک می‌سازد.
موتور تشخیص قانون‌محور و قطعی است؛ هیچ «AI» دروغینی وجود ندارد.
