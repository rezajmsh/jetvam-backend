# Jetvam Cache Infrastructure

این ماژول هم Spring Cache (`@Cacheable`, `@CacheEvict`) و هم API دستی `JetvamCache` را فراهم می‌کند.
provider پیش‌فرض `local` با Caffeine است و با یک property به Redis تغییر می‌کند.

```yaml
jetvam:
  cache:
    enabled: true
    provider: local # local | redis
    names: [i18n-messages]
    local:
      maximum-size: 10000
      expire-after-write: 10m
      record-stats: true
    redis:
      host: localhost
      port: 6379
      password: ${JETVAM_REDIS_PASSWORD:}
      database: 0
      ssl: false
      command-timeout: 3s
      default-ttl: 10m
      key-prefix: "jetvam::"
      scan-batch-size: 1000
```

مقادیر cache در حالت Redis با Java serialization ذخیره می‌شوند و بنابراین باید `Serializable` باشند.
پاک‌سازی cache در Redis با `SCAN` انجام می‌شود و از `KEYS` استفاده نمی‌کند.
