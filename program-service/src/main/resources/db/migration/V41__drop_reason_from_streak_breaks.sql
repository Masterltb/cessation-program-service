-- V19__drop_reason_from_streak_breaks.sql (tuỳ chọn)
ALTER TABLE program.streak_breaks DROP COLUMN IF EXISTS reason;
