# Jetvam Backend

Backend سامانه Jetvam به‌صورت modular monolith و با سه composition root مستقل سازمان‌دهی شده است.

## Runtime applications

| Application | Purpose |
| --- | --- |
| `jetvam-services-app` | APIهای پورتال مشتری، پذیرنده و backoffice و orchestration درخواست تسهیلات |
| `jetvam-jobs-app` | دریافت تراکنش providerها، reconciliation، settlement و jobهای زمان‌بندی‌شده |
| `jetvam-uaa-app` | احراز هویت، OTP، کاربران/نقش‌ها و OAuth2/OIDC |

## Dependency direction

```text
apps -> business modules -> shared/common
apps -> infrastructure -> shared/common
infrastructure -X-> business modules
```

`apps` محل composition و قابلیت‌های اختصاصی هر runtime است؛ زیرساخت مدیریت job که فقط توسط
`jetvam-jobs-app` استفاده می‌شود نیز در همان app قرار دارد. منطق کسب‌وکار همچنان باید در `modules`
بماند. زیرساخت‌های مشترک چند runtime از طریق ماژول‌های `infrastructure` و property prefixهای
`jetvam.*` عرضه می‌شوند.

## Infrastructure modules

| Module | Responsibility | Configuration prefix |
| --- | --- | --- |
| `jetvam-infra-persistence` | PostgreSQL، HikariCP، Hibernate و Flyway | `jetvam.persist` |
| `jetvam-infra-cache` | Spring Cache و API دستی روی Caffeine یا Redis | `jetvam.cache` |
| `jetvam-infra-i18n` | پیام‌های دیتابیسی، fallback زبان و یکپارچگی `MessageSource` | `jetvam.i18n` |
| `jetvam-infra-web` | قراردادها و تنظیمات HTTP | `jetvam.web` |
| `jetvam-infra-security` | زیرساخت امنیت | `jetvam.security` |
| `jetvam-infra-observability` | logging، metrics و tracing | `jetvam.observability` |

هر سه application فقط dependency زیرساخت و مقادیر `application.yml` را تعریف می‌کنند؛ ساخت Beanهای
`DataSource`، `CacheManager`، `Flyway` و `MessageSource` بر عهده auto-configurationها است.

## Business modules

- `identity`: Party، پروفایل مشتری، account، role، OTP و احراز هویت
- `product`: طرح تسهیلاتی عمومی/سازمانی، audience و policyهای هر طرح
- `origination`: درخواست تسهیلات، مراحل و ادامه درخواست نیمه‌تمام
- `assessment`: شاهکار، اعتبارسنجی و قواعد احراز
- `guarantee`: ضامن، ضمانت سازمانی، وثیقه و چک کاغذی
- `contract`: تولید قرارداد و امضای دیجیتال
- `payment`: پرداخت کارمزد استعلام و سایر پرداخت‌ها
- `facility`: ارسال به core، پرداخت وجه و شارژ اعتبار توسط provider
- `merchant`: پذیرنده و ترمینال‌ها
- `transaction`: دریافت و تطبیق تراکنش providerها
- `settlement`: محاسبه و اجرای تسویه پذیرنده
- `notification`: پیامک و اعلان‌های فرایندی

در ماژول identity، `PartyEntity` ریشه abstract با استراتژی JPA `JOINED` است و
`IndividualPartyEntity` و `OrganizationPartyEntity` subtypeهای آن هستند. ستون `party_type`
نقش discriminator را دارد. `CustomerProfileEntity` و `UserAccountEntity` subtype نیستند و به Party متصل می‌شوند.

## Build

Prerequisites: Java 21+ and Maven 3.6.3+.

```shell
mvn clean verify
```

Versions are centralized in the root `pom.xml`. The current baseline is Spring Boot 4.1.1 and Spring Modulith 2.1.1.

Lombok در parent پروژه تعریف شده و در تمام ماژول‌ها در دسترس است. تنظیمات مشترک آن در
`lombok.config` قرار دارد؛ برای property/data classها از annotationهای محدود مانند `@Getter` و
`@Setter` استفاده شود و از `@Data` روی entityهای JPA خودداری شود.

## Customer registration and login

ثبت‌نام مشتری و مدیریت کاربران دو use case مستقل هستند:

1. `POST /api/v1/customer/registrations/otp` با `mobile` و `nationalCode`
2. `POST /api/v1/customer/registrations/verify` با `challengeId` و `otp`
3. `PATCH /api/v1/customer/profile` برای تکمیل نام، نام خانوادگی و تاریخ تولد توسط مشتری لاگین‌شده

در مرحله دوم، شاهکار کنترل و یک account از نوع `OTP` و بدون username/password ساخته می‌شود. برای
ورود، ابتدا `POST /api/v1/customer/auth/otp` فراخوانی و سپس کد در token endpoint استاندارد exchange می‌شود:

```text
POST /oauth2/token
grant_type=urn:jetvam:params:oauth:grant-type:otp
client_id=jetvam-portal
challenge_id=<uuid>
otp=<code>
scope=jetvam.api offline_access
```

کاربران سیستمی و پذیرنده account از نوع `PASSWORD` دارند. مسیر مدیریتی
`POST /api/v1/users/parties/{partyId}/accounts` نیز برای ساخت account جدید روی Party موجود فراهم است.
تفکیک primary authentication method اجازه می‌دهد MFA بعداً به‌عنوان عامل دوم، بدون تغییر مدل Party، اضافه شود.

در محیط runtime باید `JETVAM_OTP_HMAC_SECRET` با حداقل ۳۲ کاراکتر و تنظیمات endpointهای
`JETVAM_OTP_PROVIDER_*` و `JETVAM_SHAHKAR_PROVIDER_*` از secret/configuration خارجی تأمین شوند؛
providerها به‌صورت پیش‌فرض fail-closed هستند.

## Job management

اپ `jetvam-jobs-app` مالک اجرای workloadهای پس‌زمینه است و API مدیریتی
`/api/v1/jobs` را روی پورت پیش‌فرض `8082` ارائه می‌کند. تعریف‌ها در `job_definition` و تمام اجراهای
زمان‌بندی‌شده و دستی در `job_execution` ثبت می‌شوند. Handler هر job در composition root اپ Jobs قرار
می‌گیرد و فقط سرویس ماژول بیزینسی متولی را فراخوانی می‌کند. job اولیه‌ی `notification-dispatch` نیز
از همین مسیر `NotificationDeliveryService` ماژول Notification را اجرا می‌کند. زیرساخت Quartz، مدل
مدیریت job و تاریخچه نیز به دلیل اختصاصی بودن به همین runtime داخل `jetvam-jobs-app` نگهداری می‌شوند.
هر execution تعداد کل آیتم‌های پردازش‌شده، موفق و خطادار را به‌صورت مستقل ثبت می‌کند.
