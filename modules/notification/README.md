# Jetvam Notification Module

The notification module exposes `NotificationService` for channel-neutral, durable delivery requests. Producers
enqueue a template code, destination, parameters, expiration time, and stable idempotency key. They never call
SMS, email, push, or in-app providers directly.

Messages use a transactional PostgreSQL outbox. Sensitive destinations and parameters are encrypted with
AES-256-GCM. The dispatcher claims records with a lease, retries transient failures with exponential backoff,
marks permanent failures as `DEAD`, and recovers abandoned `PROCESSING` records after their lease expires.

Required for an enabled dispatcher:

```yaml
jetvam:
  notification:
    outbox:
      encryption-key-base64: ${JETVAM_NOTIFICATION_ENCRYPTION_KEY_BASE64}
    dispatcher:
      enabled: true
    sms:
      enabled: true
      base-url: https://sms-provider.example
      path: /v1/messages
```

The decoded encryption key must contain exactly 32 random bytes. All application instances that share an
outbox must use the same key. The worker should run against the producer's datasource/schema so enqueue and
delivery remain transactionally consistent. SMS requests include the notification UUID as `Idempotency-Key`.
