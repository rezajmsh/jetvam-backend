# Jetvam I18N Infrastructure

پیام‌ها از جدول `i18n_message` خوانده و به تفکیک locale cache می‌شوند. این ماژول دو روش استفاده دارد:

```java
// API دستی
String text = messageResolver.getMessage("loan.request.created", locale, trackingCode);

// یکپارچه با Spring
String text = messageSource.getMessage("loan.request.created", args, locale);
```

Bean با نام استاندارد `messageSource` ساخته می‌شود؛ بنابراین validation، Spring MVC و سایر اجزای Spring نیز
از پیام‌های دیتابیس استفاده می‌کنند. پس از تغییر پیام توسط backoffice، `refresh(locale)` یا `refresh()` روی
`MessageResolver` فراخوانی شود.

```yaml
jetvam:
  i18n:
    enabled: true
    default-locale: fa-IR
    table-name: i18n_message
    cache-name: i18n-messages
    fallback-to-language: true
    fallback-to-default-locale: true
    use-code-as-default-message: false
```
