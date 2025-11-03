-- V6__add_entitlement_tier_at_creation.sql
ALTER TABLE program.programs
    ADD COLUMN IF NOT EXISTS entitlement_tier_at_creation text;
