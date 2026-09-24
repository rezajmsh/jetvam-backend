# قرارداد ورود فرانت‌اند

آدرس پیش‌فرض UAA برابر `http://localhost:8081` است. تمام کلاینت‌های فرانت public هستند و
`client_secret` ندارند. endpoint دریافت و refresh توکن همیشه `POST /oauth2/token` است و بدنه آن
باید `application/x-www-form-urlencoded` باشد، نه JSON.

نمونه TypeScript قابل استفاده در فرانت در
[`frontend-authentication-client.ts`](frontend-authentication-client.ts) قرار دارد.

## انتخاب جریان ورود

| نوع کاربر | روش ورود | client_id | مرحله اول | مرحله دریافت توکن |
| --- | --- | --- | --- | --- |
| مشتری | فقط موبایل و OTP | `jetvam-portal` | `POST /api/v1/customer/auth/otp` | grant نوع OTP |
| ادمین/اپراتور جت‌وام | پسورد و در صورت فعال بودن OTP | `jetvam-backoffice` | `POST /api/v1/password-users/auth/prepare` | grant نوع Password |
| ادمین/اپراتور پذیرنده | پسورد و در صورت فعال بودن OTP | `jetvam-merchant-portal` | `POST /api/v1/password-users/auth/prepare` | grant نوع Password |

فرانت نوع کاربر را از صفحه یا portal ورود می‌داند؛ برای تشخیص نوع کاربر نباید چند endpoint را
امتحان کند. پاسخ توکن شامل `access_token`، `refresh_token`، `token_type`، `expires_in` و `scope` است.

```json
{
  "access_token": "eyJ...",
  "refresh_token": "...",
  "token_type": "Bearer",
  "expires_in": 900,
  "scope": "jetvam.api offline_access"
}
```

## ورود مشتری موجود

### 1. ارسال OTP

```http
POST /api/v1/customer/auth/otp HTTP/1.1
Host: localhost:8081
Content-Type: application/json

{"mobile":"09121234567"}
```

نمونه پاسخ:

```json
{
  "challengeId": "7df2f6cf-7548-4f76-a984-beb46a682589",
  "expiresAt": "2026-09-23T17:35:00Z",
  "resendAvailableAt": "2026-09-23T17:31:00Z"
}
```

فرانت باید `challengeId` را موقت نگه دارد و صفحه ورود کد پیامک را نمایش دهد.

### 2. تبدیل OTP به توکن

```http
POST /oauth2/token HTTP/1.1
Host: localhost:8081
Content-Type: application/x-www-form-urlencoded

client_id=jetvam-portal&grant_type=urn%3Ajetvam%3Aparams%3Aoauth%3Agrant-type%3Aotp&challenge_id=7df2f6cf-7548-4f76-a984-beb46a682589&otp=123456&scope=jetvam.api%20offline_access
```

مشتری هیچ‌وقت username/password ندارد. مشتری جدید ابتدا از
`POST /api/v1/customer/registrations/otp` و سپس `/registrations/verify` استفاده می‌کند؛ بعد از
ثبت‌نام برای گرفتن توکن همین جریان ورود مشتری را انجام می‌دهد.

## ورود ادمین یا اپراتور جت‌وام

### 1. بررسی پسورد و سیاست 2FA

```http
POST /api/v1/password-users/auth/prepare HTTP/1.1
Host: localhost:8081
Content-Type: application/json

{"username":"operator","password":"correct-password"}
```

اگر 2FA خاموش باشد:

```json
{
  "secondFactorRequired": false,
  "challengeId": null,
  "expiresAt": null,
  "resendAvailableAt": null
}
```

اگر 2FA روشن باشد، OTP ارسال می‌شود:

```json
{
  "secondFactorRequired": true,
  "challengeId": "ef524cba-4021-474d-8d75-64be30b6e111",
  "expiresAt": "2026-09-23T17:35:00Z",
  "resendAvailableAt": "2026-09-23T17:31:00Z"
}
```

### 2. دریافت توکن بدون 2FA

```http
POST /oauth2/token HTTP/1.1
Host: localhost:8081
Content-Type: application/x-www-form-urlencoded

client_id=jetvam-backoffice&grant_type=urn%3Ajetvam%3Aparams%3Aoauth%3Agrant-type%3Apassword&username=operator&password=correct-password&scope=jetvam.api%20offline_access
```

### 3. دریافت توکن با 2FA

اگر `secondFactorRequired=true` بود، فرانت باید کد OTP را از کاربر بگیرد و `challenge_id` و `otp`
را با هم ارسال کند:

```http
POST /oauth2/token HTTP/1.1
Host: localhost:8081
Content-Type: application/x-www-form-urlencoded

client_id=jetvam-backoffice&grant_type=urn%3Ajetvam%3Aparams%3Aoauth%3Agrant-type%3Apassword&username=operator&password=correct-password&challenge_id=ef524cba-4021-474d-8d75-64be30b6e111&otp=123456&scope=jetvam.api%20offline_access
```

تنظیم این گروه `security.system-users.two-factor-required` است.

## ورود ادمین یا اپراتور پذیرنده

جریان دقیقاً مانند کاربران جت‌وام است؛ فقط `client_id` باید `jetvam-merchant-portal` باشد. endpoint
prepare همچنان `/api/v1/password-users/auth/prepare` است. تنظیم این گروه
`security.merchant-users.two-factor-required` است.

```http
POST /oauth2/token HTTP/1.1
Host: localhost:8081
Content-Type: application/x-www-form-urlencoded

client_id=jetvam-merchant-portal&grant_type=urn%3Ajetvam%3Aparams%3Aoauth%3Agrant-type%3Apassword&username=merchant.operator&password=correct-password&scope=jetvam.api%20offline_access
```

در صورت نیاز به 2FA، همان `challenge_id` و `otp` مرحله قبل به فرم بالا اضافه می‌شوند.

## استفاده و refresh توکن

توکن دسترسی در تمام APIهای محافظت‌شده به این شکل ارسال می‌شود:

```http
Authorization: Bearer <access_token>
```

برای refresh، همان client مربوط به جریان ورود باید استفاده شود:

```http
POST /oauth2/token HTTP/1.1
Host: localhost:8081
Content-Type: application/x-www-form-urlencoded

client_id=jetvam-backoffice&grant_type=refresh_token&refresh_token=<refresh-token>
```

Refresh token چرخشی است. فرانت باید refresh token جدید پاسخ را جایگزین قبلی کند. نگهداری refresh
token در `localStorage` ممنوع است؛ از secure platform storage یا session سمت BFF استفاده شود.

JWT شامل claimهای `user_id`، `party_id`، `categories`، `roles`، `permissions` و `amr` است. فرانت
می‌تواند نقش‌ها را برای نمایش UI استفاده کند، اما تصمیم امنیتی نهایی همیشه در backend انجام می‌شود.

## خطاهای مهم برای فرانت

- `invalid_grant`: پسورد، OTP، challenge یا وضعیت حساب معتبر نیست؛ پیام عمومی ورود ناموفق نمایش داده شود.
- `invalid_request`: پارامتر لازم وجود ندارد یا `challenge_id` و `otp` با هم ارسال نشده‌اند.
- `invalid_client` یا `unauthorized_client`: `client_id` با portal انتخاب‌شده سازگار نیست.
- `invalid_scope`: scope خارج از `jetvam.api offline_access` درخواست شده است.

## مجوز سمت سرویس

هر عملیات محافظت‌شده باید هم role و هم permission را کنترل کند:

```java
@PreAuthorize("hasRole('CUSTOMER') and hasAuthority('profile:write:self')")
@PreAuthorize("hasAnyRole('MERCHANT_ADMIN', 'MERCHANT_OPERATOR') and hasAuthority('merchant:read:self')")
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR') and hasAuthority('identity:user:read')")
```

permissionهای دارای `:self` به‌تنهایی مالکیت را اثبات نمی‌کنند. سرویس بیزینسی باید `partyId` کاربر
را با مالک resource مقایسه کند یا merchant/branch boundary را در query اعمال کند.

## مدیریت تنظیمات 2FA

فقط `SYSTEM_ADMIN` دارای permission مناسب می‌تواند تنظیمات را بخواند یا تغییر دهد:

```http
GET /api/v1/settings
Authorization: Bearer <system-admin-token>
```

```http
PATCH /api/v1/settings/security.system-users.two-factor-required
Authorization: Bearer <system-admin-token>
Content-Type: application/json

{"value":"true"}
```
