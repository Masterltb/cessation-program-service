-- V24__quiz_personalization.sql
-- Chuẩn hoá: enums, template scope/owner, question/choice, assignments (loại bỏ user_id)

-----------------------------
-- SCHEMA & ENUMS
-----------------------------
CREATE SCHEMA IF NOT EXISTS program;

-- program.question_type
DO $do$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace
    WHERE n.nspname='program' AND t.typname='question_type'
  ) THEN
    EXECUTE 'CREATE TYPE program.question_type AS ENUM (''SINGLE_CHOICE'',''MULTI_CHOICE'',''TRUE_FALSE'',''SHORT_TEXT'')';
END IF;
END $do$;

-- program.assignment_scope
DO $do$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace
    WHERE n.nspname='program' AND t.typname='assignment_scope'
  ) THEN
    EXECUTE 'CREATE TYPE program.assignment_scope AS ENUM (''DAY'',''WEEK'',''ONCE'')';
END IF;
END $do$;

-- program.quiz_assignment_origin
DO $do$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace
    WHERE n.nspname='program' AND t.typname='quiz_assignment_origin'
  ) THEN
    EXECUTE 'CREATE TYPE program.quiz_assignment_origin AS ENUM (''SYSTEM'',''COACH_CUSTOM'')';
END IF;
END $do$;

-----------------------------
-- QUIZ TEMPLATES
-----------------------------
ALTER TABLE program.quiz_templates
    ADD COLUMN IF NOT EXISTS scope    varchar(20) NOT NULL DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS owner_id uuid;

-- dọn cột cũ
DO $do$
BEGIN
BEGIN ALTER TABLE program.quiz_templates DROP COLUMN owner_type; EXCEPTION WHEN undefined_column THEN NULL; END;
END $do$;

-- gỡ UNIQUE cũ dạng uq_quiz_template*
DO $do$
DECLARE r record;
BEGIN
FOR r IN
SELECT conname
FROM pg_constraint
WHERE conrelid='program.quiz_templates'::regclass
      AND contype='u'
      AND conname LIKE 'uq_quiz_template%'
  LOOP
    EXECUTE format('ALTER TABLE program.quiz_templates DROP CONSTRAINT %I', r.conname);
END LOOP;
END $do$;

-- UNIQUE mới
ALTER TABLE program.quiz_templates
    ADD CONSTRAINT uq_quiz_template_name_scope_owner_version
        UNIQUE (name, scope, owner_id, version);

CREATE INDEX IF NOT EXISTS idx_quiz_template_scope_owner
    ON program.quiz_templates(scope, owner_id);

-----------------------------
-- QUIZ TEMPLATE QUESTIONS
-----------------------------
-- chuẩn tên cột question_text
DO $do$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='quiz_template_questions' AND column_name='text'
  ) THEN
    EXECUTE 'ALTER TABLE program.quiz_template_questions RENAME COLUMN "text" TO question_text';
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='quiz_template_questions' AND column_name='question_text'
  ) THEN
    EXECUTE 'ALTER TABLE program.quiz_template_questions ADD COLUMN question_text text';
END IF;

  -- chuẩn tên cột type
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='quiz_template_questions' AND column_name='type'
  ) AND EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='quiz_template_questions' AND column_name='question_type'
  ) THEN
    EXECUTE 'ALTER TABLE program.quiz_template_questions RENAME COLUMN question_type TO type';
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='quiz_template_questions' AND column_name='type'
  ) AND EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='quiz_template_questions' AND column_name='q_type'
  ) THEN
    EXECUTE 'ALTER TABLE program.quiz_template_questions RENAME COLUMN q_type TO type';
END IF;

  -- thêm type dạng text khi chưa có
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='quiz_template_questions' AND column_name='type'
  ) THEN
    EXECUTE 'ALTER TABLE program.quiz_template_questions ADD COLUMN type text';
EXECUTE 'UPDATE program.quiz_template_questions SET type = ''SINGLE_CHOICE'' WHERE type IS NULL';
END IF;

  -- bảo đảm points, explanation tồn tại
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='quiz_template_questions' AND column_name='points'
  ) THEN
    EXECUTE 'ALTER TABLE program.quiz_template_questions ADD COLUMN points int';
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='quiz_template_questions' AND column_name='explanation'
  ) THEN
    EXECUTE 'ALTER TABLE program.quiz_template_questions ADD COLUMN explanation text';
END IF;
END $do$;

-- kiểu dữ liệu
ALTER TABLE program.quiz_template_questions
ALTER COLUMN question_text TYPE text;

-- ép cột type sang enum
DO $do$
DECLARE v_schema text; v_name text;
BEGIN
SELECT udt_schema, udt_name
INTO v_schema, v_name
FROM information_schema.columns
WHERE table_schema='program' AND table_name='quiz_template_questions' AND column_name='type';

IF v_schema IS NULL OR (v_schema='pg_catalog' AND (v_name='text' OR v_name='varchar')) THEN
    EXECUTE 'ALTER TABLE program.quiz_template_questions
             ALTER COLUMN type TYPE program.question_type
             USING type::text::program.question_type';
END IF;
END $do$;

-- FK về templates
DO $do$
BEGIN
  PERFORM 1 FROM pg_constraint WHERE conname='fk_qtq_template';
  IF NOT FOUND THEN
    EXECUTE 'ALTER TABLE program.quiz_template_questions
             ADD CONSTRAINT fk_qtq_template
             FOREIGN KEY (template_id)
             REFERENCES program.quiz_templates(id)
             ON DELETE CASCADE';
END IF;
END $do$;

-----------------------------
-- QUIZ CHOICE LABELS
-----------------------------
DO $do$
BEGIN
  -- đổi label -> label_text
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='quiz_choice_labels' AND column_name='label'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='quiz_choice_labels' AND column_name='label_text'
  ) THEN
    EXECUTE 'ALTER TABLE program.quiz_choice_labels RENAME COLUMN label TO label_text';
END IF;

  -- tạo label_code từ score nếu có score
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='quiz_choice_labels' AND column_name='score'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='quiz_choice_labels' AND column_name='label_code'
  ) THEN
    EXECUTE 'ALTER TABLE program.quiz_choice_labels ADD COLUMN label_code varchar(8)';
EXECUTE 'UPDATE program.quiz_choice_labels SET label_code = score::text WHERE label_code IS NULL';
BEGIN
EXECUTE 'ALTER TABLE program.quiz_choice_labels DROP COLUMN score';
EXCEPTION WHEN undefined_column THEN NULL;
END;
END IF;

  -- bảo đảm có label_code
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='quiz_choice_labels' AND column_name='label_code'
  ) THEN
    EXECUTE 'ALTER TABLE program.quiz_choice_labels ADD COLUMN label_code varchar(8)';
END IF;
END $do$;

ALTER TABLE program.quiz_choice_labels
ALTER COLUMN label_code TYPE varchar(8);

-- PK (template_id, question_no, label_code)
DO $do$
BEGIN
BEGIN
EXECUTE 'ALTER TABLE program.quiz_choice_labels DROP CONSTRAINT quiz_choice_labels_pkey';
EXCEPTION WHEN undefined_object THEN NULL;
END;

  PERFORM 1 FROM pg_constraint WHERE conname='pk_quiz_choice_labels';
  IF NOT FOUND THEN
    EXECUTE 'ALTER TABLE program.quiz_choice_labels
             ADD CONSTRAINT pk_quiz_choice_labels
             PRIMARY KEY (template_id, question_no, label_code)';
END IF;
END $do$;

-- FK (template_id, question_no) -> quiz_template_questions
DO $do$
BEGIN
  PERFORM 1 FROM pg_constraint WHERE conname='fk_qcl_question';
  IF NOT FOUND THEN
    EXECUTE 'ALTER TABLE program.quiz_choice_labels
             ADD CONSTRAINT fk_qcl_question
             FOREIGN KEY (template_id, question_no)
             REFERENCES program.quiz_template_questions(template_id, question_no)
             ON DELETE CASCADE';
END IF;
END $do$;

-----------------------------
-- QUIZ ASSIGNMENTS (loại bỏ user_id, chuẩn cột mới)
-----------------------------
ALTER TABLE program.quiz_assignments
    ADD COLUMN IF NOT EXISTS scope       program.assignment_scope       NOT NULL DEFAULT 'DAY',
    ADD COLUMN IF NOT EXISTS origin      program.quiz_assignment_origin NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN IF NOT EXISTS expires_at  timestamptz,
    ADD COLUMN IF NOT EXISTS every_days  int,
    ADD COLUMN IF NOT EXISTS created_at  timestamptz NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS created_by  uuid;

DO $do$
BEGIN
BEGIN ALTER TABLE program.quiz_assignments DROP COLUMN user_id;      EXCEPTION WHEN undefined_column THEN NULL; END;
BEGIN ALTER TABLE program.quiz_assignments DROP COLUMN assigned_at;  EXCEPTION WHEN undefined_column THEN NULL; END;
BEGIN ALTER TABLE program.quiz_assignments DROP COLUMN due_at;       EXCEPTION WHEN undefined_column THEN NULL; END;
BEGIN ALTER TABLE program.quiz_assignments DROP COLUMN status;       EXCEPTION WHEN undefined_column THEN NULL; END;
BEGIN ALTER TABLE program.quiz_assignments DROP COLUMN note;         EXCEPTION WHEN undefined_column THEN NULL; END;
END $do$;

-- bảo đảm cột tham chiếu tối thiểu
DO $do$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='quiz_assignments' AND column_name='template_id'
  ) THEN
    EXECUTE 'ALTER TABLE program.quiz_assignments ADD COLUMN template_id uuid';
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='quiz_assignments' AND column_name='program_id'
  ) THEN
    EXECUTE 'ALTER TABLE program.quiz_assignments ADD COLUMN program_id uuid';
END IF;
END $do$;
