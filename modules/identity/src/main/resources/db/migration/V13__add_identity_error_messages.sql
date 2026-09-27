insert into i18n_message (message_key, locale, message_text, description)
values ('error.identity-customer-registration-required', 'fa-IR',
        'برای ورود، ابتدا ثبت‌نام مشتری را تکمیل کنید.',
        'Customer login attempted before registration completion'),
       ('error.identity-customer-registration-required', 'en-US',
        'Customer registration must be completed before signing in.',
        'Customer login attempted before registration completion'),
       ('error.identity-customer-account-unavailable', 'fa-IR',
        'حساب مشتری در حال حاضر امکان ورود ندارد؛ با پشتیبانی تماس بگیرید.',
        'Customer account is pending, disabled or locked'),
       ('error.identity-customer-account-unavailable', 'en-US',
        'Customer account is not available for sign-in; contact support.',
        'Customer account is pending, disabled or locked');
