# راهنمای جامع احراز هویت کلاینت‌های جت‌وام

این سند قرارداد ورود، دریافت توکن و تمدید نشست برای پرتال مشتری، پنل عملیاتی جت‌وام و پنل پذیرنده
است. تیم‌های وب، موبایل و BFF باید جریان‌های این سند را بدون تلاش برای تشخیص نوع کاربر از طریق چند
endpoint پیاده‌سازی کنند.

- نسخه قرارداد: `1.0`
- تاریخ به‌روزرسانی: `2026-09-24`
- سرویس صادرکننده توکن: `jetvam-uaa`

## 1. اطلاعات پایه

آدرس محیط local:

```text
http://localhost:8081
```

در محیط‌های دیگر، مقدار `UAA_BASE_URL` توسط تنظیمات همان محیط تعیین می‌شود. تمام ارتباطات محیط‌های
غیر local باید روی HTTPS انجام شوند.

endpoint صدور و تمدید توکن:

```text
POST {UAA_BASE_URL}/oauth2/token
```

بدنه درخواست token endpoint همیشه باید با این content type ارسال شود:

```http
Content-Type: application/x-www-form-urlencoded
```

بدنه token endpoint نباید JSON باشد. endpointهای `/api/v1/**` که در ادامه آمده‌اند JSON هستند.

کلاینت‌های فعلی public هستند و `client_secret` ندارند. قرار دادن client secret داخل JavaScript،
اپلیکیشن موبایل یا هر کلاینت قابل نصب ممنوع است.

## 2. انتخاب جریان براساس پرتال

| پرتال | دسته کاربر | نقش‌های اصلی | روش ورود | `client_id` |
| --- | --- | --- | --- | --- |
| پرتال مشتری | `CUSTOMER` | `CUSTOMER` | شماره موبایل و OTP | `jetvam-portal` |
| پنل جت‌وام | `OPERATOR` | `SYSTEM_ADMIN`، `SYSTEM_OPERATOR` | پسورد و 2FA اختیاری | `jetvam-backoffice` |
| پنل پذیرنده | `MERCHANT` | `MERCHANT_ADMIN`، `MERCHANT_OPERATOR` | پسورد و 2FA اختیاری | `jetvam-merchant-portal` |

نکات مهم:

- پرتال از قبل می‌داند برای کدام دسته کاربر ساخته شده و باید `client_id` درست را انتخاب کند.
- برای تشخیص نوع کاربر، درخواست ورود را با چند `client_id` تکرار نکنید.
- نقش عمومی `MERCHANT_USER` نیز ممکن است برای حساب‌های محدود پذیرنده وجود داشته باشد؛ پنل‌های
  مدیریتی و عملیاتی باید به‌ترتیب از `MERCHANT_ADMIN` و `MERCHANT_OPERATOR` استفاده کنند.
- نقش `UAA_ADMIN` legacy است و برای توسعه جدید استفاده نمی‌شود.
- حساب‌های دسته `SERVICE` ورود تعاملی فرانت ندارند و در این قرارداد پوشش داده نشده‌اند.

## 3. ثابت‌های موردنیاز کلاینت

```text
OTP_GRANT      = urn:jetvam:params:oauth:grant-type:otp
PASSWORD_GRANT = urn:jetvam:params:oauth:grant-type:password
DEFAULT_SCOPE  = jetvam.api offline_access
```

فرانت باید grant typeها را دقیقاً با همین مقدار ارسال کند.

## 4. پاسخ موفق token endpoint

دو قالب پاسخ مستقل وجود دارد:

- `/oauth2/token` پاسخ استاندارد OAuth و بدون envelope برمی‌گرداند.
- endpointهای `/api/v1/**` پاسخ را داخل envelope مشترک `{success,data,error,meta}` برمی‌گردانند.

در ورود موفق یا refresh موفق، پاسخ HTTP 200 به شکل زیر است:

```json
{
  "access_token": "eyJ...",
  "refresh_token": "...",
  "token_type": "Bearer",
  "expires_in": 900,
  "scope": "jetvam.api offline_access"
}
```

| فیلد | توضیح |
| --- | --- |
| `access_token` | JWT مورد استفاده برای فراخوانی APIهای محافظت‌شده |
| `refresh_token` | توکن یک‌بارمصرف برای تمدید نشست |
| `token_type` | در حال حاضر همیشه `Bearer` |
| `expires_in` | عمر access token برحسب ثانیه |
| `scope` | scopeهای صادرشده برای نشست |

عمر پیش‌فرض access token پانزده دقیقه و refresh token سی روز است، اما کلاینت نباید این اعداد را
hard-code کند؛ همیشه از `expires_in` پاسخ استفاده شود.

## 5. ورود مشتری موجود با موبایل و OTP

مشتری username و password ندارد. ورود مشتری دو مرحله دارد.

### 5.1. درخواست OTP

```http
POST /api/v1/customer/auth/otp HTTP/1.1
Host: localhost:8081
Content-Type: application/json

{
  "mobile": "09121234567"
}
```

پاسخ موفق HTTP 202:

```json
{
  "success": true,
  "data": {
    "challengeId": "7df2f6cf-7548-4f76-a984-beb46a682589",
    "expiresAt": "2026-09-24T17:35:00Z",
    "resendAvailableAt": "2026-09-24T17:31:00Z"
  },
  "error": null,
  "meta": {
    "timestamp": "2026-09-24T17:30:00Z",
    "requestId": "8b317657-2d10-4d5b-931e-f8bcc81c39fd",
    "traceId": "b13d0d6e2f164f66a276aaca691e7438"
  }
}
```

فرانت باید:

1. `data.challengeId` را فقط تا پایان جریان ورود در حافظه موقت نگه دارد.
2. زمان انقضا را با `data.expiresAt` نمایش دهد.
3. دکمه ارسال مجدد را قبل از `data.resendAvailableAt` فعال نکند.
4. از متن پاسخ برای تشخیص وجود یا عدم وجود حساب استفاده نکند.

### 5.2. تبدیل OTP به توکن

```http
POST /oauth2/token HTTP/1.1
Host: localhost:8081
Content-Type: application/x-www-form-urlencoded

client_id=jetvam-portal&grant_type=urn%3Ajetvam%3Aparams%3Aoauth%3Agrant-type%3Aotp&challenge_id=7df2f6cf-7548-4f76-a984-beb46a682589&otp=123456&scope=jetvam.api%20offline_access
```

پارامترها:

| پارامتر | اجباری | مقدار |
| --- | --- | --- |
| `client_id` | بله | `jetvam-portal` |
| `grant_type` | بله | `urn:jetvam:params:oauth:grant-type:otp` |
| `challenge_id` | بله | شناسه مرحله قبل |
| `otp` | بله | کد واردشده توسط مشتری |
| `scope` | پیشنهاد می‌شود | `jetvam.api offline_access` |

OTP موفق فقط یک‌بار قابل مصرف است. برای تلاش مجدد پس از مصرف یا انقضا باید challenge جدید گرفته شود.

## 6. ثبت‌نام مشتری جدید

این بخش فقط برای مشتری‌ای است که هنوز حساب ندارد. ثبت‌نام توکن صادر نمی‌کند؛ پس از ثبت‌نام، مشتری
از جریان ورود بخش 5 استفاده می‌کند.

### 6.1. ارسال OTP ثبت‌نام

```http
POST /api/v1/customer/registrations/otp HTTP/1.1
Host: localhost:8081
Content-Type: application/json

{
  "mobile": "09121234567",
  "nationalCode": "0067749828"
}
```

پاسخ HTTP 202 همان ساختار challenge بخش قبل را دارد.

### 6.2. تأیید OTP و مالکیت سیم‌کارت

```http
POST /api/v1/customer/registrations/verify HTTP/1.1
Host: localhost:8081
Content-Type: application/json

{
  "challengeId": "7df2f6cf-7548-4f76-a984-beb46a682589",
  "otp": "123456"
}
```

در این مرحله backend استعلام شاهکار را انجام می‌دهد. پاسخ موفق HTTP 201:

```json
{
  "success": true,
  "data": {
    "userId": "1599f089-a189-431f-8f53-e019f2a0ad6d",
    "partyId": "d2b02e34-790f-4485-82aa-d194041e609a",
    "onboardingStatus": "IDENTITY_VERIFIED"
  },
  "error": null,
  "meta": {
    "timestamp": "2026-09-24T17:31:00Z",
    "requestId": "3b6e3134-a80a-4bcc-b0ae-957df30ddc12",
    "traceId": "a67b7054f39749ef85c89aa7524ca8ea"
  }
}
```

بعد از موفقیت، فرانت باید جریان ورود مشتری را از `POST /api/v1/customer/auth/otp` شروع کند. پاسخ
ثبت‌نام به‌تنهایی نشست authenticated ایجاد نمی‌کند.

### 6.3. تکمیل پروفایل بعد از ورود

پس از دریافت access token مشتری:

```http
PATCH /api/v1/customer/profile HTTP/1.1
Host: localhost:8081
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "firstName": "علی",
  "lastName": "احمدی",
  "birthDate": "1990-05-20"
}
```

## 7. ورود ادمین و اپراتور جت‌وام

برای هر دو نقش `SYSTEM_ADMIN` و `SYSTEM_OPERATOR` از `jetvam-backoffice` استفاده می‌شود. فرانت
setting مربوط به 2FA را جداگانه نمی‌خواند؛ token endpoint بعد از اعتبارسنجی پسورد تصمیم می‌گیرد.

setting سمت سرور:

```text
security.system-users.two-factor-required
```

### 7.1. درخواست اولیه ورود

```http
POST /oauth2/token HTTP/1.1
Host: localhost:8081
Content-Type: application/x-www-form-urlencoded

client_id=jetvam-backoffice&grant_type=urn%3Ajetvam%3Aparams%3Aoauth%3Agrant-type%3Apassword&username=operator&password=correct-password&scope=jetvam.api%20offline_access
```

اگر 2FA خاموش باشد، همین درخواست پاسخ HTTP 200 و توکن‌ها را برمی‌گرداند. ورود در یک درخواست تمام
می‌شود.

اگر 2FA روشن باشد و پسورد معتبر باشد، OTP ارسال و پاسخ HTTP 400 زیر برگردانده می‌شود:

```json
{
  "error": "second_factor_required",
  "error_description": "A one-time password is required to complete authentication",
  "challenge_id": "ef524cba-4021-474d-8d75-64be30b6e111",
  "expires_at": "2026-09-24T17:35:00Z",
  "resend_available_at": "2026-09-24T17:31:00Z"
}
```

`second_factor_required` یک شکست ورود نیست؛ یک وضعیت میانی مورد انتظار است. فرانت باید صفحه OTP را
نمایش دهد و `challenge_id` را نگه دارد.

### 7.2. تکمیل ورود دومرحله‌ای

```http
POST /oauth2/token HTTP/1.1
Host: localhost:8081
Content-Type: application/x-www-form-urlencoded

client_id=jetvam-backoffice&grant_type=urn%3Ajetvam%3Aparams%3Aoauth%3Agrant-type%3Apassword&username=operator&password=correct-password&challenge_id=ef524cba-4021-474d-8d75-64be30b6e111&otp=123456&scope=jetvam.api%20offline_access
```

در درخواست دوم، `username` و `password` نیز دوباره ارسال می‌شوند. `challenge_id` و `otp` باید همیشه
با هم وجود داشته باشند.

## 8. ورود ادمین و اپراتور پذیرنده

جریان دقیقاً مشابه بخش 7 است، با این تفاوت‌ها:

| مورد | مقدار |
| --- | --- |
| `client_id` | `jetvam-merchant-portal` |
| category | `MERCHANT` |
| نقش‌ها | `MERCHANT_ADMIN` یا `MERCHANT_OPERATOR` |
| setting مربوط به 2FA | `security.merchant-users.two-factor-required` |

درخواست اولیه:

```http
POST /oauth2/token HTTP/1.1
Host: localhost:8081
Content-Type: application/x-www-form-urlencoded

client_id=jetvam-merchant-portal&grant_type=urn%3Ajetvam%3Aparams%3Aoauth%3Agrant-type%3Apassword&username=merchant.operator&password=correct-password&scope=jetvam.api%20offline_access
```

در صورت دریافت `second_factor_required`، همان درخواست با `challenge_id` و `otp` تکرار می‌شود.

## 9. الگوریتم پیشنهادی فرانت برای ورود پسوردی

```text
ارسال username/password به /oauth2/token
                │
                ├─ HTTP 200
                │    └─ ذخیره امن توکن‌ها و ورود به پنل
                │
                ├─ error = second_factor_required
                │    ├─ نگهداری موقت challenge_id
                │    ├─ نمایش صفحه OTP
                │    └─ تکرار /oauth2/token با challenge_id + otp
                │
                └─ سایر errorها
                     └─ نمایش خطای مناسب و عدم ورود
```

نمونه TypeScript آماده در
[`frontend-authentication-client.ts`](frontend-authentication-client.ts) قرار دارد.

## 10. ارسال access token به APIها

تمام APIهای محافظت‌شده باید header زیر را دریافت کنند:

```http
Authorization: Bearer <access-token>
```

کلاینت نباید access token را در query string، URL، log یا ابزار analytics قرار دهد.

## 11. تمدید نشست با refresh token

برای refresh باید دقیقاً همان `client_id` ورود اولیه استفاده شود.

```http
POST /oauth2/token HTTP/1.1
Host: localhost:8081
Content-Type: application/x-www-form-urlencoded

client_id=jetvam-backoffice&grant_type=refresh_token&refresh_token=<refresh-token>
```

نمونه برای مشتری فقط در `client_id` تفاوت دارد:

```text
client_id=jetvam-portal
```

Refresh tokenها چرخشی هستند. بعد از refresh موفق:

1. access token قبلی جایگزین شود.
2. refresh token قبلی حذف شود.
3. refresh token جدید پاسخ به‌صورت اتمیک ذخیره شود.
4. اگر refresh ناموفق بود، یک بار دیگر با refresh token مصرف‌شده تلاش نشود و کاربر به صفحه ورود
   هدایت شود.

برای جلوگیری از چند refresh هم‌زمان، کلاینت باید درخواست‌های refresh را single-flight کند؛ یعنی در
هر لحظه فقط یک refresh فعال باشد و سایر درخواست‌ها منتظر نتیجه همان درخواست بمانند.

## 12. claimهای JWT

access token علاوه بر claimهای استاندارد JWT شامل موارد زیر است:

| claim | نمونه | کاربرد |
| --- | --- | --- |
| `user_id` | UUID | شناسه حساب کاربری |
| `party_id` | UUID | شناسه شخص یا پذیرنده مالک حساب |
| `categories` | `["OPERATOR"]` | context کسب‌وکاری حساب |
| `roles` | `["SYSTEM_ADMIN"]` | نقش‌های کاربر |
| `permissions` | `["identity:user:read"]` | مجوزهای ریزدانه |
| `amr` | `["pwd"]` یا `["pwd","otp"]` | روش‌های استفاده‌شده برای ورود |

در ورود مشتری مقدار `amr` شامل `otp` است. در ورود پسوردی تک‌مرحله‌ای `pwd` و در ورود دومرحله‌ای
`pwd` و `otp` ثبت می‌شوند.

فرانت می‌تواند role و permission را برای نمایش یا پنهان‌کردن اجزای UI استفاده کند، اما نباید آن‌ها
را مبنای امنیت نهایی قرار دهد. مجوز نهایی همیشه در backend کنترل می‌شود.

## 13. مدیریت خطاها

### 13.1. خطاهای token endpoint

| `error` | معنی | رفتار پیشنهادی فرانت |
| --- | --- | --- |
| `second_factor_required` | پسورد معتبر است و OTP لازم است | نمایش صفحه OTP با challenge پاسخ |
| `invalid_grant` | پسورد، OTP، challenge یا وضعیت حساب معتبر نیست | پیام عمومی «اطلاعات ورود معتبر نیست» |
| `invalid_request` | پارامتر لازم وجود ندارد یا پارامتر تکراری است | ثبت خطای فنی و اصلاح request کلاینت |
| `invalid_client` | `client_id` معتبر یا مجاز نیست | توقف جریان و ثبت خطای پیکربندی |
| `unauthorized_client` | کلاینت اجازه استفاده از grant را ندارد | توقف جریان و ثبت خطای پیکربندی |
| `invalid_scope` | scope درخواستی مجاز نیست | استفاده از `jetvam.api offline_access` |
| `temporarily_unavailable` | فعلاً امکان صدور OTP جدید نیست | غیرفعال‌کردن ارسال مجدد و تلاش بعدی |
| `server_error` | خطای داخلی صادرکننده توکن | پیام موقت و ثبت correlation اطلاعات درخواست |

برای جلوگیری از user enumeration، در خطای `invalid_grant` نباید به کاربر گفته شود username، پسورد،
OTP یا وضعیت حساب دقیقاً کدام‌یک نامعتبر بوده است.

### 13.2. خطاهای REST مربوط به OTP و ثبت‌نام

endpointهای `/api/v1/**` از envelope عمومی API استفاده می‌کنند. کلاینت باید HTTP status و `error.code`
را بررسی کند. وضعیت‌های مهم:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "COMMON.VALIDATION_FAILED",
    "message": "Request validation failed",
    "violations": [],
    "details": {}
  },
  "meta": {
    "timestamp": "2026-09-24T17:32:00Z",
    "requestId": "47eb65b1-d50c-4abe-9df5-3dc29cc567ec",
    "traceId": "89a27bef79a0435fb34535903779051b"
  }
}
```

| HTTP status | معنی معمول |
| --- | --- |
| `400` | داده ورودی یا OTP نامعتبر |
| `401` | access token وجود ندارد یا معتبر نیست |
| `403` | کاربر authenticated است ولی permission لازم را ندارد |
| `409` | حساب یا هویت متعارض/تکراری است |
| `429` | محدودیت درخواست OTP رد شده است |
| `500/503` | خطای موقت سرور یا سرویس بیرونی |

## 14. نگهداری امن توکن و credential

- پسورد و OTP هرگز در log، analytics، crash report یا error monitoring ثبت نشوند.
- refresh token در `localStorage` نگهداری نشود.
- در معماری وب ترجیحی، refresh token داخل session امن BFF یا cookie با ویژگی‌های `HttpOnly`،
  `Secure` و `SameSite` مناسب نگهداری شود.
- در موبایل از secure storage سیستم‌عامل استفاده شود.
- access token فقط به دامنه‌های مورد اعتماد جت‌وام ارسال شود.
- پاسخ `second_factor_required` و challenge نباید بین کاربران یا tabهای مختلف به اشتراک گذاشته شود.
- با خروج کاربر، tokenها و تمام stateهای موقت شامل `challenge_id` از کلاینت پاک شوند.
- کلاینت نباید ساعت محلی را منبع قطعی اعتبار token بداند؛ پاسخ 401 سرور باید مدیریت شود.

## 15. چک‌لیست تحویل کلاینت

- [ ] انتخاب `client_id` براساس پرتال، نه براساس حدس از username
- [ ] ارسال token request با `application/x-www-form-urlencoded`
- [ ] پشتیبانی از پاسخ `second_factor_required` به‌عنوان وضعیت میانی
- [ ] ارسال هم‌زمان `challenge_id` و `otp`
- [ ] استفاده از `expires_in` به‌جای TTL ثابت
- [ ] refresh چرخشی و جایگزینی اتمیک refresh token
- [ ] جلوگیری از refresh هم‌زمان
- [ ] ارسال access token فقط در header استاندارد Authorization
- [ ] عدم ذخیره credential یا token در log و analytics
- [ ] عدم اتکا به roleهای سمت فرانت برای امنیت واقعی

## 16. خلاصه بسیار کوتاه برای پیاده‌سازی

```text
مشتری:
  POST /api/v1/customer/auth/otp
  POST /oauth2/token با client_id=jetvam-portal و OTP_GRANT

کاربر پنل جت‌وام:
  POST /oauth2/token با client_id=jetvam-backoffice و PASSWORD_GRANT
  اگر second_factor_required بود، همان request با challenge_id و otp تکرار شود

کاربر پنل پذیرنده:
  POST /oauth2/token با client_id=jetvam-merchant-portal و PASSWORD_GRANT
  اگر second_factor_required بود، همان request با challenge_id و otp تکرار شود
```
