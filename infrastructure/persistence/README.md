# Jetvam Persistence Infrastructure

این ماژول با اضافه‌شدن به classpath، `DataSource` مبتنی بر HikariCP، تنظیمات Hibernate و Flyway را
از prefix برابر `jetvam.persist` به‌صورت خودکار فعال می‌کند. دیتابیس پیش‌فرض PostgreSQL است.

```yaml
jetvam:
  persist:
    url: jdbc:postgresql://localhost:5432/jetvam
    username: jetvam
    password: ${JETVAM_DB_PASSWORD}
    default-schema: jetvam
    pool:
      minimum-idle: 5
      maximum-pool-size: 30
      connection-timeout: 30s
      max-lifetime: 30m
    jpa:
      ddl-auto: validate
      show-sql: false
      format-sql: false
      jdbc-batch-size: 50
    migration:
      enabled: true
      locations: classpath:db/migration
```

Hibernate فقط schema را validate می‌کند؛ ایجاد و تغییر جداول باید منحصراً با migrationهای Flyway انجام شود.

برای entityهای معمول، `AbstractUuidEntity` شناسه UUID و optimistic locking و
`AbstractAuditableUuidEntity` علاوه بر آن `created_at` و `updated_at` را فراهم می‌کند.
