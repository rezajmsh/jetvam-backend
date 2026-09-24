ALTER TABLE iam_user_account ADD COLUMN authentication_mobile varchar(11);

UPDATE iam_user_account account
SET authentication_mobile = individual.mobile
FROM iam_individual_party individual
WHERE account.party_id = individual.id
  AND account.primary_authentication_method = 'PASSWORD';

CREATE INDEX ix_iam_user_authentication_mobile
    ON iam_user_account(authentication_mobile)
    WHERE authentication_mobile IS NOT NULL;
