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

`apps` فقط محل assemble کردن ماژول‌ها و تنظیمات runtime است. منطق کسب‌وکار باید در `modules` بماند. ماژول‌های infrastructure که نیاز به راه‌اندازی خودکار دارند، در گام پیاده‌سازی با Spring Boot auto-configuration و property prefixهای `jetvam.*` عرضه می‌شوند.

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

- `identity`: login، OTP، token، user و role
- `cif`: پروفایل مشتری/سازمان و اطلاعات پایه KYC
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

## Build

Prerequisites: Java 21+ and Maven 3.6.3+.

```shell
mvn clean verify
```

Versions are centralized in the root `pom.xml`. The current baseline is Spring Boot 4.1.1 and Spring Modulith 2.1.1.

Lombok در parent پروژه تعریف شده و در تمام ماژول‌ها در دسترس است. تنظیمات مشترک آن در
`lombok.config` قرار دارد؛ برای property/data classها از annotationهای محدود مانند `@Getter` و
`@Setter` استفاده شود و از `@Data` روی entityهای JPA خودداری شود.
