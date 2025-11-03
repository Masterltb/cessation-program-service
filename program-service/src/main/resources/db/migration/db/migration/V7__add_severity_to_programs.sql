-- Add column for Program.severity (enum lưu dạng STRING)
ALTER TABLE program.programs
    ADD COLUMN IF NOT EXISTS severity text;
