-- Scope role uniqueness by organization
-- Drop existing global unique constraints (names may vary; include common defaults)
ALTER TABLE base."role" DROP CONSTRAINT IF EXISTS uk_role_name;
ALTER TABLE base."role" DROP CONSTRAINT IF EXISTS uk_role_code;

-- Add composite unique constraints per organization
ALTER TABLE base."role"
    ADD CONSTRAINT uk_role_name_per_org UNIQUE (org_id, role_name);
ALTER TABLE base."role"
    ADD CONSTRAINT uk_role_code_per_org UNIQUE (org_id, role_code);
