-- V5__add_current_day_to_programs.sql

-- 1) Thêm cột nếu chưa có
ALTER TABLE program.programs
    ADD COLUMN IF NOT EXISTS current_day int;

-- 2) Gán giá trị mặc định an toàn cho các dòng cũ
UPDATE program.programs
SET current_day = 0
WHERE current_day IS NULL;

-- 3) Đặt DEFAULT (giữ NOT NULL tùy nhu cầu)
ALTER TABLE program.programs
    ALTER COLUMN current_day SET DEFAULT 0;

-- (Tuỳ chọn) chỉ set NOT NULL khi chắc chắn không còn NULL
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM program.programs WHERE current_day IS NULL) THEN
ALTER TABLE program.programs
    ALTER COLUMN current_day SET NOT NULL;
END IF;
END $$;
