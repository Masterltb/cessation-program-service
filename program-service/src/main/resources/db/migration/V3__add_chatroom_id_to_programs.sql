-- V3__add_chatroom_id_to_programs.sql

-- 1) Thêm cột nếu chưa có
ALTER TABLE program.programs
    ADD COLUMN IF NOT EXISTS chatroom_id uuid;

-- 2) Chỉ thêm FK nếu:
--    a) bảng program.chatrooms tồn tại
--    b) chưa có constraint tên fk_program_chatroom
DO $$
BEGIN
  IF EXISTS (
      SELECT 1
      FROM information_schema.tables
      WHERE table_schema='program' AND table_name='chatrooms'
  ) THEN
    IF NOT EXISTS (
      SELECT 1
      FROM pg_constraint c
      JOIN pg_class t ON t.oid = c.conrelid
      JOIN pg_namespace n ON n.oid = t.relnamespace
      WHERE c.conname = 'fk_program_chatroom'
        AND n.nspname = 'program'
        AND t.relname = 'programs'
    ) THEN
ALTER TABLE program.programs
    ADD CONSTRAINT fk_program_chatroom
        FOREIGN KEY (chatroom_id) REFERENCES program.chatrooms(id);
END IF;
END IF;
END $$;
