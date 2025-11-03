-- V4__add_coach_id_to_programs.sql

-- 1) Thêm cột nếu chưa có
ALTER TABLE program.programs
    ADD COLUMN IF NOT EXISTS coach_id uuid;

-- 2) (khuyến nghị) index để query theo coach nhanh hơn
CREATE INDEX IF NOT EXISTS idx_programs_coach_id
    ON program.programs (coach_id);

-- 3) (tuỳ chọn) Thêm FK nếu có bảng tham chiếu.
-- Trường hợp coach cũng là user trong schema 'auth.users':
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema='auth' AND table_name='users'
  ) THEN
    IF NOT EXISTS (
      SELECT 1
      FROM pg_constraint c
      JOIN pg_class t ON t.oid = c.conrelid
      JOIN pg_namespace n ON n.oid = t.relnamespace
      WHERE c.conname = 'fk_programs_coach'
        AND n.nspname = 'program'
        AND t.relname = 'programs'
    ) THEN
ALTER TABLE program.programs
    ADD CONSTRAINT fk_programs_coach
        FOREIGN KEY (coach_id) REFERENCES auth.users(id);
END IF;
END IF;
END $$;

-- Nếu bạn có bảng coach riêng: đổi 'auth.users(id)' thành 'program.coaches(id)'
-- và đổi tên constraint tuỳ ý.
