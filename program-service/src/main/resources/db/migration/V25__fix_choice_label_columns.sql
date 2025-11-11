-- V25__fix_choice_label_columns.sql

-- 1) Đổi tên cột 'correct' -> 'is_correct' khi đang dùng tên cũ
DO $do$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='quiz_choice_labels' AND column_name='correct'
  ) THEN
    EXECUTE 'ALTER TABLE program.quiz_choice_labels RENAME COLUMN correct TO is_correct';
END IF;
END $do$;

-- 2) Bổ sung cột is_correct khi chưa có
ALTER TABLE program.quiz_choice_labels
    ADD COLUMN IF NOT EXISTS is_correct boolean;

-- 3) Gán giá trị mặc định để dữ liệu cũ hợp lệ, rồi ép NOT NULL
UPDATE program.quiz_choice_labels
SET is_correct = false
WHERE is_correct IS NULL;

ALTER TABLE program.quiz_choice_labels
    ALTER COLUMN is_correct SET NOT NULL;

-- 4) Đảm bảo cột weight tồn tại (entity có @Column(name="weight"))
ALTER TABLE program.quiz_choice_labels
    ADD COLUMN IF NOT EXISTS weight int;

-- 5) Đảm bảo label_text đúng tên cột (phòng trường hợp còn 'label')
DO $do$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='quiz_choice_labels' AND column_name='label_text'
  ) AND EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='quiz_choice_labels' AND column_name='label'
  ) THEN
    EXECUTE 'ALTER TABLE program.quiz_choice_labels RENAME COLUMN label TO label_text';
END IF;
END $do$;
