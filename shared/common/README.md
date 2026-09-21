# Jetvam Common

این ماژول فقط primitiveها و قراردادهای مشترک و مستقل از لایه‌های Web، Security و Persistence را نگه می‌دارد. اضافه‌کردن منطق کسب‌وکاری یا وابستگی به یک bounded context در این ماژول مجاز نیست.

## Conversion

`TypeConversions` تبدیل‌های strict و null-preserving برای `String`، اعداد، `Boolean`، `UUID`، enum و انواع اصلی `java.time` ارائه می‌دهد. مقدار `null` به `null` تبدیل می‌شود و ورودی نامعتبر `TypeConversionException` ایجاد می‌کند. تبدیل عددی silent truncation ندارد.

```java
Integer amount = TypeConversions.asInteger("۱۲۳۴");
RequestStatus status = TypeConversions.asEnum("in-progress", RequestStatus.class);
Instant createdAt = TypeConversions.asInstant("2026-09-21T11:30:00+03:30");
```

## Validation

توابع `IranianIdentifiers` برای normalize و validate کردن کد ملی، موبایل، شبا، کارت بانکی و ساختار کد پستی قابل استفاده‌اند. annotationهای متناظر نیز برای Jakarta Validation وجود دارند:

- `@IranianNationalCode`
- `@IranianMobileNumber`
- `@IranianIban`
- `@IranianBankCard`
- `@IranianPostalCode`

این annotationها مقدار `null` را معتبر می‌دانند؛ برای فیلد اجباری باید `@NotNull` یا `@NotBlank` نیز اضافه شود. `ValidationCollector` برای جمع‌کردن چند خطای domain command و برگرداندن همه آن‌ها در یک پاسخ است.

`Preconditions` برای guardهای fail-fast در کد و configuration است و مقدار معتبر را برای استفاده inline
برمی‌گرداند. این کلاس با `ValidationCollector` که مخصوص خطاهای ورودی کسب‌وکار است تفاوت دارد:

```java
String url = Preconditions.requireText(properties.getUrl(), "jetvam.persist.url");
int poolSize = Preconditions.requirePositive(properties.getMaximumPoolSize(), "maximumPoolSize");
```

## Exception hierarchy

```text
JetvamException
├── BusinessException
│   ├── ValidationException
│   │   └── TypeConversionException
│   ├── ResourceNotFoundException
│   ├── ConflictException
│   ├── OperationNotAllowedException
│   └── IllegalStateTransitionException
└── TechnicalException
    ├── IntegrationException
    └── ConfigurationException
```

Exceptionهای common به HTTP وابسته نیستند. نگاشت exception به status code و response body مسئولیت `jetvam-infra-web` است. اطلاعات حساس در `FieldViolation` نگهداری نمی‌شود.

## Date and time

- timezone رسمی پیش‌فرض محصول در `DateTimeUtils.TEHRAN_ZONE` تعریف شده است.
- برای استفاده‌های معمول، زمان جاری بدون ورودی در دسترس است: `now()`، `today()`،
  `nowInTehran()`، `currentDateTimeInTehran()` و `currentTimeInTehran()`.
- `today()` و `PersianDateUtils.today()` بر اساس timezone رسمی محصول، یعنی تهران، محاسبه می‌شوند؛
  `now()` یک `Instant` مستقل از timezone برمی‌گرداند.
- overloadهای دارای `Clock` برای منطق کسب‌وکار و تست‌های deterministic حفظ شده‌اند.
- `InstantRange` بازه نیمه‌باز `[start, end)` و `DateRange` بازه تاریخ بسته `[start, end]` است.
- `PersianDateUtils` مرز واحد تبدیل و format تاریخ شمسی/میلادی است.
- زمان ذخیره‌سازی و تبادل بین سرویس‌ها باید `Instant` و نمایش UI باید وابسته به timezone باشد.

```java
Instant now = DateTimeUtils.now();
LocalDate today = DateTimeUtils.today();
LocalDateTime tehranDateTime = DateTimeUtils.currentDateTimeInTehran();
LocalTime tehranTime = DateTimeUtils.currentTimeInTehran();
PersianDate persianToday = PersianDateUtils.today();

PersianDate persian = PersianDateUtils.parse("۱۴۰۵/۰۱/۰۱");
LocalDate gregorian = PersianDateUtils.toGregorian(persian);
InstantRange reportRange = DateTimeUtils.dayRange(gregorian, DateTimeUtils.TEHRAN_ZONE);
```

در منطق حساس به زمان (مانند سررسید، انقضا و SLA) از overload دارای `Clock` استفاده شود؛
متدهای بدون ورودی برای timestamp، نمایش، logging و مسیرهایی هستند که کنترل زمان در تست لازم نیست.
