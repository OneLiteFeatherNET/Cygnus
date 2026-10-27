-- Slender pass earning: the pity counter, the monthly quota marker and the two earned ledger reasons.
--
-- Plain SQL that PostgreSQL and H2 in PostgreSQL mode both accept. V1 and V2 are not changed; the reason check
-- of V2 is named, so it is replaced in place and the ledger rows keep their ids.

-- Survivor rounds since the last Slender round or the last earned pass. Existing balances start at zero.
ALTER TABLE player_slender_pass
    ADD COLUMN rounds_since_slender INTEGER NOT NULL DEFAULT 0 CHECK (rounds_since_slender >= 0);

-- The calendar month ('YYYY-MM', UTC) of the last monthly quota, empty until the first grant.
ALTER TABLE player_slender_pass
    ADD COLUMN last_quota_month VARCHAR(7);

-- Admit the two earned reasons next to the admin and redeem reasons.
ALTER TABLE slender_pass_transaction DROP CONSTRAINT ck_slender_pass_transaction_reason;
ALTER TABLE slender_pass_transaction ADD CONSTRAINT ck_slender_pass_transaction_reason
    CHECK (reason IN ('ADMIN_GIVE', 'ADMIN_TAKE', 'ADMIN_SET', 'REDEEM', 'EARNED_PITY', 'RANK_QUOTA'));
