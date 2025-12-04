
-- ===================================================
-- SOURCE FILE: V1__baseline_program_objects.sql
-- ===================================================
-- ===== V1: Baseline schema & core objects =====
create schema if not exists program;
alter schema program owner to program_app_rw;
set search_path to program;

-- ===== Enums =====
do $$
begin
  if not exists (
    select 1 from pg_type t join pg_namespace n on n.oid=t.typnamespace
    where n.nspname='program' and t.typname='quiz_template_status'
  ) then
create type program.quiz_template_status as enum ('DRAFT','PUBLISHED','ARCHIVED');
end if;

  if not exists (
    select 1 from pg_type t join pg_namespace n on n.oid=t.typnamespace
    where n.nspname='program' and t.typname='attempt_status'
  ) then
create type program.attempt_status as enum ('OPEN','SUBMITTED');
end if;

  if not exists (
    select 1 from pg_type t join pg_namespace n on n.oid=t.typnamespace
    where n.nspname='program' and t.typname='quiz_scope'
  ) then
create type program.quiz_scope as enum ('system','coach');
end if;

  -- thêm enum mức độ nếu còn thiếu (khớp với enum trong code Java)
  if not exists (
    select 1 from pg_type t join pg_namespace n on n.oid=t.typnamespace
    where n.nspname='program' and t.typname='severity_level'
  ) then
create type program.severity_level as enum ('LOW','MODERATE','HIGH','VERY_HIGH');
end if;
end $$;

-- ===== Core: programs (PHẢI có trước vì nhiều FK trỏ vào) =====
create table if not exists program.programs (
                                                id          uuid primary key,
                                                user_id     uuid not null,
                                                plan_days   int  not null,
                                                status      text not null,
                                                started_at  timestamptz,
                                                completed_at timestamptz,
                                                created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    deleted_at  timestamptz
    );

-- ===== Quiz templates =====
create table if not exists program.quiz_templates(
                                                     id uuid primary key,
                                                     name text not null,
                                                     version int not null default 1,
                                                     status program.quiz_template_status not null default 'DRAFT',
                                                     language_code text,
                                                     published_at timestamptz,
                                                     archived_at timestamptz,
                                                     scope program.quiz_scope not null default 'system',
                                                     owner_id uuid,
                                                     created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
    );

create table if not exists program.quiz_template_questions(
                                                              template_id uuid not null references program.quiz_templates(id) on delete cascade,
    question_no int not null,
    text text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key (template_id, question_no)
    );

create table if not exists program.quiz_choice_labels(
                                                         template_id uuid not null,
                                                         question_no int not null,
                                                         score int not null,
                                                         label text not null,
                                                         primary key (template_id, question_no, score),
    foreign key (template_id, question_no)
    references program.quiz_template_questions(template_id, question_no)
    on delete cascade
    );

-- ===== Assignments =====
create table if not exists program.quiz_assignments(
                                                       id uuid primary key,
                                                       template_id uuid not null references program.quiz_templates(id),
    program_id uuid not null references program.programs(id) on delete cascade,
    assigned_by_user_id uuid,
    period_days int,
    start_offset_day int,
    use_latest_version boolean default true,
    active boolean default true,
    created_at timestamptz not null default now(),
    created_by uuid,
    every_days int not null default 5,
    scope program.quiz_scope not null default 'system'
    );
create index if not exists idx_qas_program on program.quiz_assignments(program_id);

-- ===== Attempts =====
create table if not exists program.quiz_attempts(
                                                    id uuid primary key,
                                                    program_id uuid not null references program.programs(id) on delete cascade,
    template_id uuid not null references program.quiz_templates(id),
    user_id uuid not null,
    opened_at timestamptz not null,
    submitted_at timestamptz,
    status program.attempt_status not null default 'OPEN'
    );
create index if not exists idx_qatt_program_template
    on program.quiz_attempts(program_id, template_id, status);

-- ===== Answers (chuẩn dùng attempt_id; nếu đã có bảng cũ thì để V2 xử lý chuyển đổi) =====
create table if not exists program.quiz_answers (
                                                    attempt_id  uuid not null,
                                                    question_no int  not null,
                                                    answer      text,
                                                    created_at  timestamptz not null default now(),
    primary key (attempt_id, question_no),
    constraint fk_qans_attempt foreign key (attempt_id)
    references program.quiz_attempts(id) on delete cascade
    );

-- ===== Results =====
create table if not exists program.quiz_results(
                                                   id uuid primary key,
                                                   program_id uuid not null references program.programs(id) on delete cascade,
    template_id uuid not null references program.quiz_templates(id),
    quiz_version int not null,
    total_score int not null,
    severity program.severity_level not null,
    created_at timestamptz not null default now()
    );
create index if not exists idx_qres_program_template
    on program.quiz_results(program_id, template_id, created_at desc);

;

-- ===================================================
-- SOURCE FILE: V2__quiz_attempts_and_answers.sql
-- ===================================================
set search_path to program;

-- thêm cột attempt_id nếu thiếu
do $$
begin
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='quiz_answers' and column_name='attempt_id'
  ) then
alter table program.quiz_answers add column attempt_id uuid;
end if;
end $$;

-- TODO: backfill attempt_id từ dữ liệu thực tế của bạn
-- update program.quiz_answers qa
--   set attempt_id = <map từ (program_id, question_no, user…) sang id của quiz_attempts>;

-- sau khi backfill xong, ràng buộc & PK
do $$
begin
  if not exists (
    select 1 from information_schema.table_constraints
    where table_schema='program' and table_name='quiz_answers' and constraint_type='PRIMARY KEY'
  ) then
alter table program.quiz_answers add primary key (attempt_id, question_no);
end if;
exception when others then
  -- bỏ qua nếu đã có
end $$;

do $$
begin
alter table program.quiz_answers
    add constraint fk_qans_attempt foreign key (attempt_id)
        references program.quiz_attempts(id) on delete cascade;
exception when duplicate_object then
  -- đã tồn tại
end $$;

-- sau khi đã chắc chắn attempt_id KHÔNG NULL, mới drop program_id
-- alter table program.quiz_answers alter column attempt_id set not null;
-- alter table program.quiz_answers drop column if exists program_id;

;

-- ===================================================
-- SOURCE FILE: V3__add_chatroom_id_to_programs.sql
-- ===================================================
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

;

-- ===================================================
-- SOURCE FILE: V4__add_coach_id_to_programs.sql
-- ===================================================
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

;

-- ===================================================
-- SOURCE FILE: V5__add_current_day_to_programs.sql
-- ===================================================
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

;

-- ===================================================
-- SOURCE FILE: V6__add_entitlement_tier_at_creation.sql
-- ===================================================
-- V6__add_entitlement_tier_at_creation.sql
ALTER TABLE program.programs
    ADD COLUMN IF NOT EXISTS entitlement_tier_at_creation text;

;

-- ===================================================
-- SOURCE FILE: V7__add_severity_to_programs.sql
-- ===================================================
-- Add column for Program.severity (enum lưu dạng STRING)
ALTER TABLE program.programs
    ADD COLUMN IF NOT EXISTS severity text;

;

-- ===================================================
-- SOURCE FILE: V8__add_streak_columns_to_programs.sql
-- ===================================================
-- ===========================================
-- V8__add_streak_columns_to_programs.sql
-- Thêm cột theo dõi streak cho program.programs
-- Idempotent (AN TOÀN chạy nhiều lần)
-- ===========================================

-- (Giả sử schema program đã được tạo bởi R__ensure_program_schema.sql)

ALTER TABLE program.programs
    ADD COLUMN IF NOT EXISTS current_streak_days INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS longest_streak_days INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_smoke_at       TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS slip_count          INT NOT NULL DEFAULT 0;

COMMENT ON COLUMN program.programs.current_streak_days IS 'Số ngày sạch liên tiếp tính đến hôm nay';
COMMENT ON COLUMN program.programs.longest_streak_days IS 'Chuỗi sạch dài nhất đã đạt';
COMMENT ON COLUMN program.programs.last_smoke_at       IS 'Thời điểm gần nhất có hút (slip/relapse)';
COMMENT ON COLUMN program.programs.slip_count          IS 'Tổng số lần slip (không bắt buộc tính relapse)';

;

-- ===================================================
-- SOURCE FILE: V9__create_smoke_events.sql
-- ===================================================
-- ===========================================
-- V9__create_smoke_events.sql  (FIXED)
-- Log sự kiện SLIP/RELAPSE + enum type
-- Idempotent, an toàn chạy nhiều lần
-- ===========================================

-- 1) Tạo enum kiểu sự kiện nếu chưa có
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE n.nspname = 'program' AND t.typname = 'smoke_event_kind'
  ) THEN
    EXECUTE 'CREATE TYPE program.smoke_event_kind AS ENUM (''SLIP'', ''RELAPSE'')';
END IF;
END
$$;

-- 2) Tạo bảng nếu chưa có (id chưa set default để tránh phụ thuộc pgcrypto)
CREATE TABLE IF NOT EXISTS program.smoke_events (
                                                    id            UUID PRIMARY KEY,
                                                    program_id    UUID NOT NULL REFERENCES program.programs(id) ON DELETE CASCADE,
    user_id       UUID NOT NULL,
    occurred_at   TIMESTAMPTZ NOT NULL,
    kind          program.smoke_event_kind NOT NULL,
    puffs         INT NULL,        -- số hơi (tuỳ chọn)
    cigarettes    INT NULL,        -- số điếu (tuỳ chọn)
    reason        TEXT NULL,       -- lý do (map từ câu hỏi)
    repair_action TEXT NULL,       -- hành động sửa (map từ câu hỏi)
    repaired      BOOLEAN NULL,    -- đã làm hành động sửa ngay?
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
    );

COMMENT ON TABLE  program.smoke_events          IS 'Ghi lại các sự kiện SLIP/RELAPSE để điều chỉnh streak và lộ trình';
COMMENT ON COLUMN program.smoke_events.kind     IS 'SLIP=lỡ 1–2 hơi/điếu rồi quay lại; RELAPSE=tái hút có hệ thống';
COMMENT ON COLUMN program.smoke_events.repaired IS 'User đã thực hiện hành động sửa ngay sau slip?';

-- 3) Nếu có hàm gen_random_uuid() thì set default cho id (không cần quyền SUPERUSER)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'gen_random_uuid') THEN
    EXECUTE 'ALTER TABLE program.smoke_events ALTER COLUMN id SET DEFAULT gen_random_uuid()';
END IF;
END
$$;

-- 4) Indexes phục vụ truy vấn
CREATE INDEX IF NOT EXISTS idx_smoke_events_program_time
    ON program.smoke_events (program_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_smoke_events_user_time
    ON program.smoke_events (user_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_smoke_events_kind
    ON program.smoke_events (kind);

;

-- ===================================================
-- SOURCE FILE: V10__add_assignment_scope_and_expires.sql
-- ===================================================
-- =========================================================
-- V10__add_assignment_scope_and_expires.sql (FIXED)
-- Thêm enum scope + cột scope, expires_at cho các assignment
-- Idempotent & an toàn khi chạy nhiều lần
-- =========================================================

-- 1) Tạo enum program.assignment_scope nếu chưa có
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_type t
    JOIN pg_namespace n ON n.oid = t.typnamespace
    WHERE n.nspname = 'program' AND t.typname = 'assignment_scope'
  ) THEN
    EXECUTE 'CREATE TYPE program.assignment_scope AS ENUM (''DAY'', ''WEEK'', ''PROGRAM'', ''CUSTOM'')';
END IF;
END
$$;

-- 2) Thêm cột cho các bảng assignment nếu bảng tồn tại
DO $$
BEGIN
  -- step_assignments
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'program' AND table_name = 'step_assignments'
  ) THEN
    EXECUTE 'ALTER TABLE program.step_assignments
             ADD COLUMN IF NOT EXISTS scope program.assignment_scope NOT NULL DEFAULT ''DAY''';
EXECUTE 'ALTER TABLE program.step_assignments
             ADD COLUMN IF NOT EXISTS expires_at timestamptz NULL';

EXECUTE 'COMMENT ON COLUMN program.step_assignments.scope IS ''Phạm vi hiệu lực: DAY/WEEK/PROGRAM/CUSTOM''';
EXECUTE 'COMMENT ON COLUMN program.step_assignments.expires_at IS ''Hạn hiệu lực (nếu có)''';

EXECUTE 'CREATE INDEX IF NOT EXISTS idx_step_assignments_expires_at
             ON program.step_assignments (expires_at)';
END IF;

  -- quiz_assignments (nếu có)
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = 'program' AND table_name = 'quiz_assignments'
  ) THEN
    EXECUTE 'ALTER TABLE program.quiz_assignments
             ADD COLUMN IF NOT EXISTS scope program.assignment_scope NOT NULL DEFAULT ''DAY''';
EXECUTE 'ALTER TABLE program.quiz_assignments
             ADD COLUMN IF NOT EXISTS expires_at timestamptz NULL';

EXECUTE 'COMMENT ON COLUMN program.quiz_assignments.scope IS ''Phạm vi hiệu lực: DAY/WEEK/PROGRAM/CUSTOM''';
EXECUTE 'COMMENT ON COLUMN program.quiz_assignments.expires_at IS ''Hạn hiệu lực (nếu có)''';

EXECUTE 'CREATE INDEX IF NOT EXISTS idx_quiz_assignments_expires_at
             ON program.quiz_assignments (expires_at)';
END IF;

  -- (tuỳ dự án) nếu bạn có bảng assignment khác, lặp lại khối IF EXISTS tương tự
END
$$;

;

-- ===================================================
-- SOURCE FILE: V11__repair_streak_columns.sql
-- ===================================================
-- Idempotent: an toàn chạy nhiều lần.

-- Nếu trước đó lỡ đặt tên ngược (best_streak / current_streak) thì đổi về đúng chuẩn
do $$
begin
  if exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='best_streak'
  ) then
    execute 'alter table program.programs rename column best_streak to streak_best';
end if;

  if exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='current_streak'
  ) then
    execute 'alter table program.programs rename column current_streak to streak_current';
end if;
end $$;

-- Đảm bảo các cột streak tồn tại
alter table program.programs
    add column if not exists streak_current int not null default 0,
    add column if not exists streak_best    int not null default 0,
    add column if not exists last_smoke_at timestamptz null,
    add column if not exists streak_frozen_until timestamptz null;

-- Backfill: best >= current
update program.programs
set streak_best = greatest(coalesce(streak_best, 0), coalesce(streak_current, 0))
where coalesce(streak_current, 0) > 0;

;

-- ===================================================
-- SOURCE FILE: V12__add_event_type_to_smoke_events.sql
-- ===================================================
-- V12__add_event_type_to_smoke_events.sql
-- Tạo ENUM cho loại sự kiện hút/khôi phục (nếu chưa có)
do $$
begin
  if not exists (
    select 1
    from pg_type t join pg_namespace n on n.oid=t.typnamespace
    where n.nspname='program' and t.typname='smoke_event_type'
  ) then
    execute $ct$
create type program.smoke_event_type as enum
    ('SMOKE','RECOVERY_START','RECOVERY_SUCCESS','RECOVERY_FAIL')
    $ct$;
end if;
end $$;

-- Thêm cột event_type nếu chưa có và backfill
do $$
begin
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='smoke_events' and column_name='event_type'
  ) then
    execute 'alter table program.smoke_events add column event_type program.smoke_event_type';
execute 'update program.smoke_events set event_type = ''SMOKE'' where event_type is null';
execute 'alter table program.smoke_events alter column event_type set not null';
execute 'alter table program.smoke_events alter column event_type set default ''SMOKE''';
end if;
end $$;

;

-- ===================================================
-- SOURCE FILE: V13__add_event_at_to_smoke_events.sql
-- ===================================================
-- V13__add_event_at_to_smoke_events.sql
do $$
begin
  -- thêm cột event_at nếu chưa có
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='smoke_events' and column_name='event_at'
  ) then
    execute 'alter table program.smoke_events add column event_at timestamptz';
    -- backfill: dùng created_at (nếu có) hoặc now()
execute 'update program.smoke_events set event_at = coalesce(created_at, now())';
execute 'alter table program.smoke_events alter column event_at set not null';
end if;

  -- index phục vụ truy vấn theo program + thời gian
  if not exists (
    select 1 from pg_indexes
    where schemaname='program' and indexname='idx_smoke_events_program_event_at'
  ) then
    execute 'create index idx_smoke_events_program_event_at
             on program.smoke_events (program_id, event_at desc)';
end if;
end $$;

;

-- ===================================================
-- SOURCE FILE: V14__add_note_to_smoke_events.sql
-- ===================================================
-- V14__add_note_to_smoke_events.sql
do $$
begin
  -- Thêm cột note nếu chưa có
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='smoke_events' and column_name='note'
  ) then
    execute 'alter table program.smoke_events add column note text';
    -- nếu bạn muốn không null thì dùng 2 dòng dưới (tuỳ entity có nullable=false hay không)
    -- execute '' || 'update program.smoke_events set note = '''''' where note is null';
    -- execute '' || 'alter table program.smoke_events alter column note set not null';
end if;
end $$;

;

-- ===================================================
-- SOURCE FILE: V15__create_step_assignments.sql
-- ===================================================
-- V15__create_step_assignments.sql
do $$
begin
  if not exists (
    select 1 from information_schema.tables
    where table_schema='program' and table_name='step_assignments'
  ) then
    execute $SQL$
create table program.step_assignments (
                                          id            uuid primary key,
                                          program_id    uuid not null references program.programs(id) on delete cascade,

    -- thông tin step
                                          step_no       int  not null,                     -- thứ tự/bậc của step trong chương trình (1..N)
                                          planned_day   int  not null,                     -- ngày N trong plan (ví dụ: 1..30)
                                          status        varchar(32) not null default 'PENDING', -- map Enum STRING
                                          scheduled_at  timestamptz,                       -- thời điểm dự kiến
                                          completed_at  timestamptz,                       -- hoàn tất lúc
                                          note          text,

    -- audit
                                          created_at    timestamptz not null default now(),
                                          created_by    uuid,
                                          updated_at    timestamptz not null default now()
);
create index on program.step_assignments(program_id);
create unique index ux_step_assignments_program_step
    on program.step_assignments(program_id, step_no);
$SQL$;
end if;

  -- trigger update updated_at (tuỳ thích)
  if not exists (
    select 1 from pg_proc p
    join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'program' and p.proname = 'touch_updated_at'
  ) then
    execute $SQL$
      create or replace function program.touch_updated_at()
      returns trigger language plpgsql as $FN$
begin
        new.updated_at = now();
return new;
end $FN$;
    $SQL$;
end if;

  if not exists (
    select 1 from pg_trigger t
    join pg_class c on c.oid = t.tgrelid
    join pg_namespace n on n.oid = c.relnamespace
    where n.nspname='program' and c.relname='step_assignments' and t.tgname='trg_step_assignments_touch_updated_at'
  ) then
    execute $SQL$
create trigger trg_step_assignments_touch_updated_at
    before update on program.step_assignments
    for each row execute function program.touch_updated_at();
$SQL$;
end if;
end $$;

;

-- ===================================================
-- SOURCE FILE: V16__streak_tables.sql
-- ===================================================
-- V16__streak_tables.sql
do $$
begin
  if not exists (
    select 1 from information_schema.tables
    where table_schema='program' and table_name='streaks'
  ) then
    execute $SQL$
create table program.streaks (
                                 id uuid primary key,
                                 program_id uuid not null references program.programs(id) on delete cascade,
                                 started_at timestamptz not null,
                                 ended_at   timestamptz,
                                 length_days int,
                                 created_at timestamptz not null default now()
);
create index on program.streaks(program_id, started_at desc);
$SQL$;
end if;

  if not exists (
    select 1 from information_schema.tables
    where table_schema='program' and table_name='streak_breaks'
  ) then
    execute $SQL$
create table program.streak_breaks (
                                       id uuid primary key,
                                       streak_id uuid not null references program.streaks(id) on delete cascade,
                                       smoke_event_id uuid references program.smoke_events(id),
                                       broken_at timestamptz not null,
                                       reason text,
                                       created_at timestamptz not null default now()
);
create index on program.streak_breaks(streak_id, broken_at desc);
$SQL$;
end if;
end $$;

;

-- ===================================================
-- SOURCE FILE: V17__alter_streak_breaks.sql
-- ===================================================
-- V17__alter_streak_breaks.sql

-- 1) Đổi tên cột broken_at -> broke_at (nếu V16 đang là broken_at)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema='program' AND table_name='streak_breaks' AND column_name='broken_at'
  ) THEN
ALTER TABLE program.streak_breaks RENAME COLUMN broken_at TO broke_at;
END IF;
END$$;

-- 2) Bổ sung cột program_id, prev_streak_days
ALTER TABLE program.streak_breaks
    ADD COLUMN IF NOT EXISTS program_id uuid,
    ADD COLUMN IF NOT EXISTS prev_streak_days integer NOT NULL DEFAULT 0;

-- 3) Điền program_id từ smoke_events (nếu thiếu)
UPDATE program.streak_breaks sb
SET program_id = se.program_id
    FROM program.smoke_events se
WHERE sb.smoke_event_id = se.id AND sb.program_id IS NULL;

-- 4) Ràng buộc NOT NULL theo entity mới
ALTER TABLE program.streak_breaks
    ALTER COLUMN program_id SET NOT NULL,
ALTER COLUMN smoke_event_id SET NOT NULL;

-- 5) (tuỳ chọn) default cho created_at
ALTER TABLE program.streak_breaks
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'utc');

;

-- ===================================================
-- SOURCE FILE: V18__add_note_to_streak_breaks.sql
-- ===================================================
-- Thêm cột note cho bảng streak_breaks để khớp với entity
ALTER TABLE program.streak_breaks
    ADD COLUMN IF NOT EXISTS note text;

-- (tuỳ chọn) đảm bảo created_at có default UTC
ALTER TABLE program.streak_breaks
    ALTER COLUMN created_at SET DEFAULT (now() AT TIME ZONE 'utc');

;

-- ===================================================
-- SOURCE FILE: V19__seed_plan_templates.sql
-- ===================================================
-- ===========================
-- DEV SEED: Plan templates + steps (L1=30d, L2=45d, L3=60d)
-- Schema: program
-- ===========================

create schema if not exists program;
create extension if not exists pgcrypto;

-- 1) Bảng template & steps
create table if not exists program.plan_templates (
                                                      id          uuid primary key,
                                                      level       int  not null,
                                                      code        text not null unique,
                                                      name        text not null,
                                                      total_days  int  not null,
                                                      created_at  timestamptz not null default now()
    );

create table if not exists program.plan_steps (
                                                  id          uuid primary key,
                                                  template_id uuid not null references program.plan_templates(id) on delete cascade,
    day_no      int  not null,
    slot        time not null,
    title       text not null,
    details     text,
    max_minutes int,
    created_at  timestamptz not null default now()
    );

create index if not exists idx_plan_steps_template_day_slot
    on program.plan_steps(template_id, day_no, slot);

-- (tuỳ chọn) reset seed cũ
-- delete from program.plan_steps;
-- delete from program.plan_templates;

-- 2) Helper function (gọi trong DO, drop ở cuối)
create or replace function program.add_step(
  tid uuid, d int, hhmm text, t text, det text, mm int
) returns void
language plpgsql as $fn$
begin
insert into program.plan_steps(id, template_id, day_no, slot, title, details, max_minutes)
values (gen_random_uuid(), tid, d, (hhmm)::time, t, det, mm);
end
$fn$;

-- 3) Seed templates + steps
do $$
declare
l1 uuid := '11111111-1111-1111-1111-111111111111'; -- L1: Thức Tỉnh (30d)
  l2 uuid := '22222222-2222-2222-2222-222222222222'; -- L2: Thay Đổi (45d)
  l3 uuid := '33333333-3333-3333-3333-333333333333'; -- L3: Tự Do (60d)
begin
insert into program.plan_templates(id, level, code, name, total_days)
values
    (l1, 1, 'L1_30D', 'THỨC TỈNH (30 ngày)', 30),
    (l2, 2, 'L2_45D', 'THAY ĐỔI (45 ngày)', 45),
    (l3, 3, 'L3_60D', 'TỰ DO (60 ngày)', 60)
    on conflict (id) do nothing;

-- =========================
-- L1 — THỨC TỈNH (30 ngày)
-- =========================

-- Ngày 1
perform program.add_step(l1,1,'07:30','Check-in sáng + chọn mục tiêu 7 ngày',
    'Kéo thang thèm/stress; chọn giấc ngủ; gợi ý: Thở 3’, Uống nước, Đi bộ 3’',10);
  perform program.add_step(l1,1,'12:30','Học 4D + tạo Urge Log thử',
    'Chọn nguyên nhân & mức độ; chạy Timer 3’ để thực hành 4D',7);
  perform program.add_step(l1,1,'16:30','Lợi ích sau 24h bỏ thuốc (mini-slide + quiz)',
    '3 slide + 3 câu hỏi',5);
  perform program.add_step(l1,1,'21:00','Nhật ký 2 câu + chọn Mantra',
    'Viết 1–2 câu; chọn câu động lực (có giọng đọc)',12);

  -- Ngày 2
  perform program.add_step(l1,2,'07:30','Checklist dọn môi trường + nhắn người thân',
    'Bỏ bật lửa/tàn thuốc; giặt áo ám mùi; nhắn người thân',12);
  perform program.add_step(l1,2,'12:30','Urge Log + Timer 3’ (4D)',
    'Chọn nguyên nhân/mức độ; vượt cơn 3’',8);
  perform program.add_step(l1,2,'16:30','CBT nhanh: thay câu nghĩ “một điếu không sao”',
    'Điền A–B–C và câu thay thế thực tế hơn',12);
  perform program.add_step(l1,2,'21:00','Tổng kết + Mantra','Viết ngắn; đặt câu cho ngày mai',12);

  -- Ngày 3
  perform program.add_step(l1,3,'07:30','Check-in + thở hướng dẫn 5’','Nếu thèm ≥7, nghe thở 5’',12);
  perform program.add_step(l1,3,'12:30','Kịch bản xã hội','Cà phê/đồng nghiệp/một mình – chọn câu từ chối',10);
  perform program.add_step(l1,3,'16:30','Mindfulness body-scan 5’','Nghe hướng dẫn, ghi 1 câu cảm nhận',12);
  perform program.add_step(l1,3,'21:00','Tổng kết + Mantra',null,12);

  -- Ngày 4
  perform program.add_step(l1,4,'07:30','Heatmap giờ hay thèm + áp lịch nhắc','Chọn 2–3 khung giờ nhắc',10);
  perform program.add_step(l1,4,'12:30','Chọn vật thay thế tay–miệng 24h','Kẹo the/tăm/đồ bóp tay',8);
  perform program.add_step(l1,4,'16:30','1 phiên Pomodoro 25–5 không thuốc',null,12);
  perform program.add_step(l1,4,'21:00','Tổng kết + Mantra',null,12);

  -- Ngày 5
  perform program.add_step(l1,5,'07:30','Tuyên ngôn “mình là người không hút” + 1 hành động',
    'Ví dụ: 2 chai nước / đi bộ 10’',10);
  perform program.add_step(l1,5,'12:30','Urge Log + mini-quiz tim mạch','2 câu',10);
  perform program.add_step(l1,5,'16:30','Tập nói “không” (nghe mẫu + chọn câu)',null,12);
  perform program.add_step(l1,5,'21:00','Tổng kết + Mantra',null,12);

  -- Ngày 6
  perform program.add_step(l1,6,'07:30','Slip? Flow phục hồi','Chọn lý do và 1 hành động sửa; không reset toàn bộ streak',12);
  perform program.add_step(l1,6,'12:30','Kế hoạch cuối tuần không khói',null,10);
  perform program.add_step(l1,6,'16:30','Mindfulness 5’ + động viên',null,12);
  perform program.add_step(l1,6,'21:00','Tổng kết + Mantra',null,12);

  -- Ngày 7
  perform program.add_step(l1,7,'07:30','Khảo sát 7 ngày + Check-in',null,12);
  perform program.add_step(l1,7,'12:30','Rút kinh nghiệm: Top-3 chiến lược',null,10);
  perform program.add_step(l1,7,'16:30','Chọn phần thưởng nhỏ',null,10);
  perform program.add_step(l1,7,'21:00','Kế hoạch tuần sau','Chọn trọng tâm (ngủ/xã hội/Pomodoro...)',12);

  -- Ngày 8..14
  perform program.add_step(l1,8,'07:30','Check-in sáng',null,10);
  perform program.add_step(l1,8,'12:30','Urge Log',null,8);
  perform program.add_step(l1,8,'16:30','Bộ giảm stress 10’','Thở, giãn cơ, đi bộ ngắn',12);
  perform program.add_step(l1,8,'21:00','Journal + Mantra',null,12);

  perform program.add_step(l1,9,'07:30','Check-in sáng',null,10);
  perform program.add_step(l1,9,'12:30','Urge Log',null,8);
  perform program.add_step(l1,9,'16:30','5 thói quen ngủ','Giảm màn hình; tránh cafe sau 15h; phòng mát/thoáng',10);
  perform program.add_step(l1,9,'21:00','Journal + Mantra',null,12);

  perform program.add_step(l1,10,'07:30','Check-in sáng',null,10);
  perform program.add_step(l1,10,'12:30','Urge Log',null,8);
  perform program.add_step(l1,10,'16:30','Đi bộ/giãn cơ 10’','Đặt timer; hướng 1000 bước',12);
  perform program.add_step(l1,10,'21:00','Journal + Mantra',null,12);

  perform program.add_step(l1,11,'07:30','Check-in sáng',null,10);
  perform program.add_step(l1,11,'12:30','Kịch bản xã hội','Chọn tình huống + câu nói',12);
  perform program.add_step(l1,11,'16:30','Urge Log',null,8);
  perform program.add_step(l1,11,'21:00','Journal + Mantra',null,12);

  perform program.add_step(l1,12,'07:30','Check-in sáng',null,10);
  perform program.add_step(l1,12,'12:30','Urge Log',null,8);
  perform program.add_step(l1,12,'16:30','Pomodoro 25–5',null,12);
  perform program.add_step(l1,12,'21:00','Journal + Mantra',null,12);

  perform program.add_step(l1,13,'07:30','Khẳng định bản sắc + 1 hành động',null,10);
  perform program.add_step(l1,13,'12:30','Urge Log',null,8);
  perform program.add_step(l1,13,'16:30','Từ chối khéo','Nghe mẫu + chọn câu',12);
  perform program.add_step(l1,13,'21:00','Journal + Mantra',null,12);

  perform program.add_step(l1,14,'07:30','Check-in tổng tuần',null,10);
  perform program.add_step(l1,14,'12:30','Top-3 chiến lược hiệu quả',null,10);
  perform program.add_step(l1,14,'16:30','Phần thưởng nhỏ',null,10);
  perform program.add_step(l1,14,'21:00','Chọn trọng tâm tuần tới',null,12);

  -- Ngày 15..21 (loop)
for i in 15..21 loop
    perform program.add_step(l1,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l1,i,'12:30','Tình huống rủi ro cao','Chọn kịch bản khó + phương án ứng phó',12);
    perform program.add_step(l1,i,'16:30','Theo dõi thói quen thay thế','Đánh dấu uống nước/đi bộ/giãn cơ',12);
    perform program.add_step(l1,i,'21:00','Journal + Mantra',null,12);
end loop;

  -- Ngày 22..29 (loop)
for i in 22..29 loop
    perform program.add_step(l1,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l1,i,'12:30','Cập nhật tiền tiết kiệm','Nhập số tiền không chi cho thuốc hôm nay',8);
    perform program.add_step(l1,i,'16:30','Câu chuyện thành công + mini-quiz','Đọc 1 câu chuyện; trả lời 2 câu',10);
    perform program.add_step(l1,i,'21:00','Journal + Mantra',null,12);
end loop;

  -- Ngày 30
  perform program.add_step(l1,30,'07:30','Tổng kết tiến trình 30 ngày',
    'Xem huy hiệu/điểm/tiền tiết kiệm/heatmap',12);
  perform program.add_step(l1,30,'12:30','Cập nhật kế hoạch phòng tái','3 bẫy lớn + 3 hành động sẵn',12);
  perform program.add_step(l1,30,'21:00','Chọn lộ trình tiếp theo','Duy trì hoặc chuyển Level 2',12);

  -- =========================
  -- L2 — THAY ĐỔI (45 ngày)
  -- =========================
  perform program.add_step(l2,1,'07:30','Check-in + tư vấn NRT phối hợp',
    'Đánh dấu dùng miếng dán/gum (tham khảo bác sĩ)',12);
  perform program.add_step(l2,1,'12:30','Học 4D + log thử','Tạo Urge log, Timer 3’',8);
  perform program.add_step(l2,1,'16:30','CBT 1 tình huống thật','Điền A–B–C + câu thay thế',12);
  perform program.add_step(l2,1,'21:00','Nhật ký + Mantra',null,12);

  perform program.add_step(l2,2,'07:30','Chọn liều NRT tham khảo','Luôn hỏi bác sĩ khi cần',12);
  perform program.add_step(l2,2,'12:30','Urge + Timer 3’',null,10);
  perform program.add_step(l2,2,'16:30','Body-scan 5’',null,12);
  perform program.add_step(l2,2,'21:00','Nhật ký + an toàn NRT','Tick triệu chứng nhẹ nếu có',12);

  perform program.add_step(l2,3,'07:30','Check-in + thở 5’',null,12);
  perform program.add_step(l2,3,'12:30','Tình huống xã hội',null,12);
  perform program.add_step(l2,3,'16:30','Thay khung nhận thức',null,12);
  perform program.add_step(l2,3,'21:00','Nhật ký + safety NRT',null,12);

  perform program.add_step(l2,4,'07:30','Áp lịch nhắc cá nhân hoá',null,10);
  perform program.add_step(l2,4,'12:30','Vật thay thế 24h',null,8);
  perform program.add_step(l2,4,'16:30','Pomodoro 25–5',null,12);
  perform program.add_step(l2,4,'21:00','Nhật ký + Mantra',null,12);

  perform program.add_step(l2,5,'07:30','Tuyên ngôn hành động',null,10);
  perform program.add_step(l2,5,'12:30','Urge + Quiz 2 câu',null,10);
  perform program.add_step(l2,5,'16:30','Từ chối khéo',null,12);
  perform program.add_step(l2,5,'21:00','Nhật ký + Mantra',null,12);

  perform program.add_step(l2,6,'07:30','Flow phục hồi','Không reset toàn bộ streak',12);
  perform program.add_step(l2,6,'12:30','Kế hoạch cuối tuần',null,10);
  perform program.add_step(l2,6,'16:30','Mindfulness 5’',null,12);
  perform program.add_step(l2,6,'21:00','Nhật ký + safety',null,12);

  perform program.add_step(l2,7,'07:30','7-day PPA',null,12);
  perform program.add_step(l2,7,'12:30','Top-3 chiến lược',null,10);
  perform program.add_step(l2,7,'16:30','Phần thưởng nhỏ',null,10);
  perform program.add_step(l2,7,'21:00','Kế hoạch tuần 2',null,12);

  -- Ngày 8..21
for i in 8..21 loop
    perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l2,i,'12:30','Urge log','Timer 3’ nếu cần',8);
    perform program.add_step(l2,i,'16:30','Kỹ năng phù hợp','Ngủ/giảm stress/exercise/CBT',12);
    perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
end loop;

  -- Ngày 22..28
for i in 22..28 loop
    perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l2,i,'12:30','Luân phiên thói quen','Pomodoro/mindfulness/exercise/xã hội',12);
    perform program.add_step(l2,i,'16:30','Cập nhật tiền tiết kiệm',null,8);
    perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
end loop;
update program.plan_steps
set title='Tổng kết tuần + phần thưởng'
where template_id=l2 and day_no=28 and slot='21:00';

-- Ngày 29..35
for i in 29..35 loop
    perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l2,i,'12:30','Luân phiên thói quen',null,12);
    perform program.add_step(l2,i,'16:30','Cập nhật tiền tiết kiệm',null,8);
    perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
end loop;
update program.plan_steps set title='Tổng kết tuần + phần thưởng'
where template_id=l2 and day_no=35 and slot='21:00';

-- Ngày 36..42
for i in 36..42 loop
    perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l2,i,'12:30','Luân phiên thói quen',null,12);
    perform program.add_step(l2,i,'16:30','Cập nhật tiền tiết kiệm',null,8);
    perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
end loop;
update program.plan_steps set title='Tổng kết tuần + phần thưởng'
where template_id=l2 and day_no=42 and slot='21:00';

-- Ngày 43..45
for i in 43..45 loop
    perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l2,i,'12:30','Luân phiên thói quen',null,12);
    perform program.add_step(l2,i,'16:30','Cập nhật tiền tiết kiệm',null,8);
    perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
end loop;
update program.plan_steps set title='Tổng kết tuần + phần thưởng'
where template_id=l2 and day_no=45 and slot='21:00';

-- =========================
-- L3 — TỰ DO (60 ngày)
-- =========================

-- Ngày 1..3
perform program.add_step(l3,1,'07:30','Check-in nâng cao + tư vấn thuốc','Đánh dấu thuốc/NRT; tuỳ chọn đo mạch',15);
  perform program.add_step(l3,1,'12:30','Checklist môi trường + nhắn người thân',null,10);
  perform program.add_step(l3,1,'16:30','CBT-ABC (case nặng)',null,12);
  perform program.add_step(l3,1,'21:00','Nhật ký + Mantra',null,12);

  perform program.add_step(l3,2,'07:30','Kế hoạch thuốc','Giờ dán miếng dán; gum dự phòng',12);
  perform program.add_step(l3,2,'12:30','Urge baseline + đặt nhắc',null,10);
  perform program.add_step(l3,2,'16:30','Mindfulness 5’ + đi bộ 5’',null,12);
  perform program.add_step(l3,2,'21:00','Nhật ký + safety baseline',null,12);

  perform program.add_step(l3,3,'07:30','Check-in + thở 5’',null,12);
  perform program.add_step(l3,3,'12:30','Urge + Timer 3’ + gọi bạn hỗ trợ','Nếu thèm ≥8, bấm gọi buddy',10);
  perform program.add_step(l3,3,'16:30','CBT thay niềm tin lõi','Viết 1 câu thay thế mạnh',12);
  perform program.add_step(l3,3,'21:00','Nhật ký + kiểm tra tác dụng phụ',null,12);

  -- Ngày 4..14
for i in 4..14 loop
    perform program.add_step(l3,i,'07:30','Check-in sáng (nhắc dày tuần đầu)',null,12);
    perform program.add_step(l3,i,'12:30','Urge Log + 4D','Timer 3’',10);
    perform program.add_step(l3,i,'16:30','Luân phiên bài phù hợp','Thở/mindfulness/pomodoro/xã hội',12);
    perform program.add_step(l3,i,'21:00','Nhật ký + safety (nếu dùng thuốc)',null,12);
end loop;

  -- Ngày 15..28
for i in 15..28 loop
    perform program.add_step(l3,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l3,i,'12:30','Tình huống rủi ro cao','Chuẩn bị câu nói/động tác',12);
    perform program.add_step(l3,i,'16:30','Vận động nhẹ 10’','Đi bộ/giãn cơ',12);
    perform program.add_step(l3,i,'21:00','Nhật ký + Mantra',null,12);
end loop;

  -- Ngày 29..42
for i in 29..42 loop
    perform program.add_step(l3,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l3,i,'12:30','Kỹ năng xã hội nâng cao','Nói “không” khi đi nhậu/cà phê',12);
    perform program.add_step(l3,i,'16:30','Pomodoro 25–5',null,12);
    perform program.add_step(l3,i,'21:00','Nhật ký + Mantra',null,12);
end loop;

  -- Ngày 43..60
for i in 43..60 loop
    perform program.add_step(l3,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l3,i,'12:30','Kế hoạch phòng ngừa tái sử dụng','Chọn 1 bẫy + 1 hành động sẵn sàng',12);
    perform program.add_step(l3,i,'16:30','Củng cố bản sắc + mục tiêu thể lực','Đặt mục tiêu bước chân/đi bộ',12);
    perform program.add_step(l3,i,'21:00','Tổng kết tuần/huân chương','Nếu trùng mốc tuần thì trao huy hiệu',12);
end loop;

end $$;

-- 4) Dọn helper
drop function if exists program.add_step(uuid,int,text,text,text,int);

-- 5) Verify nhanh
select day_no, slot, title
from program.plan_steps
where template_id = '11111111-1111-1111-1111-111111111111' and day_no=1
order by slot;

select t.code, t.total_days, count(s.*) as step_count
from program.plan_templates t
         left join program.plan_steps s on s.template_id=t.id
group by 1,2
order by 1;

;

-- ===================================================
-- SOURCE FILE: V20__program_content_modules.sql
-- ===================================================
-- V20__program_content_modules.sql

create schema if not exists program;

-- Bảng module nội dung (JSONB + version + i18n)
create table if not exists program.content_modules (
                                                       id         uuid primary key default gen_random_uuid(),
    code       text not null,                 -- ví dụ: 'EDU_BENEFITS_24H'
    type       text not null,                 -- ví dụ: 'EDU_SLIDES_QUIZ'
    lang       text not null default 'vi',    -- 'vi' | 'en' | ...
    version    int  not null default 1,
    payload    jsonb not null,                -- slides/quiz/audio...
    updated_at timestamptz not null default now(),
    unique (code, lang, version)
    );
create index if not exists idx_content_modules_code_lang_ver
    on program.content_modules(code, lang, version desc);
create index if not exists idx_content_modules_code_lang
    on program.content_modules(code, lang);

-- Thêm module_code vào plan_steps (nếu chưa có)
alter table program.plan_steps
    add column if not exists module_code text;

-- Tuỳ chọn: với các step “EDU…/QUIZ…” gán module_code thay cho details
-- update program.plan_steps
-- set module_code = details, details = null
-- where details is not null and details ~ '^[A-Z0-9_]+$';

;

-- ===================================================
-- SOURCE FILE: V21__seed_content_modules_and_link_plan_steps.sql
-- ===================================================
-- V21__seed_content_modules_and_link_plan_steps.sql
-- Mục tiêu:
-- - Bảng content_modules (JSONB, version, lang)
-- - Bổ sung module_code cho plan_steps
-- - Seed module + link vào các bước của L1
-- - Sửa thiếu 1 step ở L1 ngày 30 (slot 16:30)
-- - Verify

BEGIN;

CREATE SCHEMA IF NOT EXISTS program;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1) Bảng content_modules
CREATE TABLE IF NOT EXISTS program.content_modules (
                                                       id         uuid PRIMARY KEY,
                                                       code       text NOT NULL,
                                                       type       text NOT NULL,
                                                       lang       text NOT NULL DEFAULT 'vi',
                                                       version    integer NOT NULL DEFAULT 1,
                                                       payload    jsonb NOT NULL,
                                                       updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_content_modules UNIQUE (code, lang, version)
    );

CREATE INDEX IF NOT EXISTS idx_cm_code_lang_ver
    ON program.content_modules(code, lang, version);

-- 2) Bổ sung cột module_code cho plan_steps
ALTER TABLE program.plan_steps
    ADD COLUMN IF NOT EXISTS module_code text;

-- 3) Helper upsert function cho content_modules (idempotent)
CREATE OR REPLACE FUNCTION program.put_module(
  p_code text, p_type text, p_lang text, p_version int, p_payload jsonb
) RETURNS uuid
LANGUAGE plpgsql AS $$
DECLARE rid uuid;
BEGIN
SELECT id INTO rid
FROM program.content_modules
WHERE code=p_code AND lang=p_lang AND version=p_version;

IF rid IS NOT NULL THEN
    -- giữ nguyên để idempotent
    RETURN rid;
END IF;

INSERT INTO program.content_modules(id, code, type, lang, version, payload, updated_at)
VALUES (gen_random_uuid(), p_code, p_type, p_lang, p_version, p_payload, now())
    RETURNING id INTO rid;

RETURN rid;
END $$;

-- 4) Seed các module (VI). Có thể mở rộng EN nếu cần
-- 4.1 Urge Log 4D (TASK)
SELECT program.put_module(
               'TASK_URGELOG_4D','TASK','vi',1,
               '{
                 "title":"Urge Log + 4D",
                 "howto":["Nhận diện cơn thèm (0-10)","Delay 3 phút","Deep breathing","Drink/Do: uống nước/đi bộ ngắn"],
                 "timerSeconds":180,
                 "fields":["trigger","intensity","note"]
               }'::jsonb
       );

-- 4.2 Lợi ích 24h (EDU_SLIDES) v1 & v2
SELECT program.put_module(
               'EDU_BENEFITS_24H','EDU_SLIDES','vi',1,
               '{
                 "title":"Lợi ích sau 24 giờ bỏ thuốc",
                 "slides":[
                   {"h":"Tim mạch","bullets":["Huyết áp ổn định hơn","Nhịp tim dần bình thường"]},
                   {"h":"Hô hấp","bullets":["CO giảm về mức bình thường","Oxy trong máu tăng lên"]}
                 ],
                 "quiz":[{"q":"Sau 24h, CO thay đổi thế nào?","options":["Tăng","Giảm về bình thường"],"answer":1}]
               }'::jsonb
       );
SELECT program.put_module(
               'EDU_BENEFITS_24H','EDU_SLIDES','vi',2,
               '{
                 "title":"Lợi ích sau 24 giờ (v2)",
                 "slides":[
                   {"h":"Tim mạch","bullets":["Huyết áp giảm","Giảm co thắt mạch"]},
                   {"h":"Hô hấp","bullets":["CO giảm rõ","Oxy tăng"]}
                 ],
                 "cta":{"text":"Tiếp tục lộ trình","deeplink":"app://plan/next"}
               }'::jsonb
       );

-- 4.3 Body scan 5 phút (AUDIO)
SELECT program.put_module(
               'MINDSL_BODYSCAN_5M','AUDIO','vi',1,
               '{
                 "title":"Mindfulness body-scan 5 phút",
                 "audioUrl":"https://cdn.example.com/audio/bodyscan-5m.mp3",
                 "transcript":"Hướng dẫn body-scan từ đầu đến chân",
                 "durationSec":300
               }'::jsonb
       );

-- 4.4 Social scripts (EDU_TEMPLATES)
SELECT program.put_module(
               'EDU_SOCIAL_SCRIPTS','EDU_TEMPLATES','vi',1,
               '{
                 "title":"Kịch bản xã hội",
                 "scenes":[
                   {"name":"Cà phê","lines":["Cảm ơn, mình đang cai thuốc","Cho mình cốc nước nhé"]},
                   {"name":"Đồng nghiệp","lines":["Mình nghỉ thuốc rồi","Ra ngoài hít thở chút thay vì hút"]}
                 ]
               }'::jsonb
       );

-- 4.5 Pomodoro 25–5 (TASK)
SELECT program.put_module(
               'TASK_POMODORO_25_5','TASK','vi',1,
               '{
                 "title":"Pomodoro 25–5",
                 "workMin":25,"breakMin":5,
                 "tips":["Rời chỗ ngồi khi giải lao","Không kèm điếu thuốc"],
                 "timer":true
               }'::jsonb
       );

-- 4.6 Bộ giảm stress 10 phút (PACK)
SELECT program.put_module(
               'PACK_STRESS_10M','PACK','vi',1,
               '{
                 "title":"Bộ giảm stress 10 phút",
                 "items":[
                   {"type":"breath","label":"Thở 4-7-8 (2p)"},
                   {"type":"stretch","label":"Giãn cơ cổ vai (3p)"},
                   {"type":"walk","label":"Đi bộ chậm (5p)"}
                 ]
               }'::jsonb
       );

-- 4.7 5 thói quen ngủ (EDU_LIST)
SELECT program.put_module(
               'EDU_SLEEP_5HABITS','EDU_LIST','vi',1,
               '{
                 "title":"5 thói quen ngủ",
                 "items":[
                   "Giảm màn hình 1 giờ trước khi ngủ",
                   "Tránh cà phê sau 15h",
                   "Phòng mát/thoáng",
                   "Giữ giờ ngủ cố định",
                   "Thư giãn ngắn trước khi ngủ"
                 ]
               }'::jsonb
       );

-- 4.8 Đi bộ/giãn cơ 10 phút (TASK)
SELECT program.put_module(
               'TASK_WALK_10M','TASK','vi',1,
               '{
                 "title":"Vận động nhẹ 10 phút",
                 "actions":["Đi bộ 1000 bước hoặc giãn cơ toàn thân"],
                 "timerSeconds":600
               }'::jsonb
       );

-- 4.9 Từ chối khéo (EDU_TEMPLATES)
SELECT program.put_module(
               'SOCIAL_SAY_NO','EDU_TEMPLATES','vi',1,
               '{
                 "title":"Tập nói \"không\"",
                 "phrases":["Mình nghỉ thuốc rồi, cảm ơn","Mình ra hít thở chút nhé"]
               }'::jsonb
       );

-- 4.10 Tình huống rủi ro cao (EDU_PLANNER)
SELECT program.put_module(
               'HIGH_RISK_SCENARIOS','EDU_PLANNER','vi',1,
               '{
                 "title":"Tình huống rủi ro cao",
                 "prompts":["Sau bữa ăn","Căng thẳng công việc","Khi buồn chán"],
                 "planField":"Phương án ứng phó"
               }'::jsonb
       );

-- 4.11 Câu chuyện thành công + quiz (EDU_STORY)
SELECT program.put_module(
               'STORY_SUCCESS_MINIQUIZ','EDU_STORY','vi',1,
               '{
                 "title":"Câu chuyện thành công",
                 "story":"Sau 3 tuần, A hết thèm khi uống nước và đi bộ ngắn.",
                 "quiz":[{"q":"Chiến lược hiệu quả nhất của A?","options":["Ngủ nhiều hơn","Uống nước + đi bộ"],"answer":1}]
               }'::jsonb
       );

-- 4.12 Kế hoạch phòng tái sử dụng (EDU_PLANNER)
SELECT program.put_module(
               'RELAPSE_PREVENTION_PLAN','EDU_PLANNER','vi',1,
               '{
                 "title":"Kế hoạch phòng tái sử dụng",
                 "traps":["Tiệc với bạn","Căng thẳng","Một điếu không sao"],
                 "actions":["Mang nước","Gọi buddy","Thở 3 phút + rời chỗ"]
               }'::jsonb
       );

-- (tùy chọn) vài bản EN mẫu
SELECT program.put_module(
               'EDU_BENEFITS_24H','EDU_SLIDES','en',1,
               '{"title":"24h Benefits","slides":[{"h":"Cardio","bullets":["BP stabilizes"]}]}'::jsonb
       );


-- 5) Gắn module_code vào step của L1
DO $$
DECLARE l1 uuid := '11111111-1111-1111-1111-111111111111';
    i int;
BEGIN
  -- Ngày 1
UPDATE program.plan_steps SET module_code='TASK_URGELOG_4D'
WHERE template_id=l1 AND day_no=1 AND slot='12:30'::time AND (module_code IS NULL OR module_code='');
UPDATE program.plan_steps SET module_code='EDU_BENEFITS_24H'
WHERE template_id=l1 AND day_no=1 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');

-- Ngày 3
UPDATE program.plan_steps SET module_code='EDU_SOCIAL_SCRIPTS'
WHERE template_id=l1 AND day_no=3 AND slot='12:30'::time AND (module_code IS NULL OR module_code='');
UPDATE program.plan_steps SET module_code='MINDSL_BODYSCAN_5M'
WHERE template_id=l1 AND day_no=3 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');

-- Ngày 4
UPDATE program.plan_steps SET module_code='TASK_POMODORO_25_5'
WHERE template_id=l1 AND day_no=4 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');

-- Ngày 8..14
UPDATE program.plan_steps SET module_code='PACK_STRESS_10M'
WHERE template_id=l1 AND day_no=8 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');
UPDATE program.plan_steps SET module_code='EDU_SLEEP_5HABITS'
WHERE template_id=l1 AND day_no=9 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');
UPDATE program.plan_steps SET module_code='TASK_WALK_10M'
WHERE template_id=l1 AND day_no=10 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');
UPDATE program.plan_steps SET module_code='EDU_SOCIAL_SCRIPTS'
WHERE template_id=l1 AND day_no=11 AND slot='12:30'::time AND (module_code IS NULL OR module_code='');
UPDATE program.plan_steps SET module_code='TASK_POMODORO_25_5'
WHERE template_id=l1 AND day_no=12 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');
UPDATE program.plan_steps SET module_code='SOCIAL_SAY_NO'
WHERE template_id=l1 AND day_no=13 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');

-- Ngày 15..21: 12:30 = rủi ro cao
FOR i IN 15..21 LOOP
UPDATE program.plan_steps SET module_code='HIGH_RISK_SCENARIOS'
WHERE template_id=l1 AND day_no=i AND slot='12:30'::time AND (module_code IS NULL OR module_code='');
END LOOP;

  -- Ngày 22..29: 16:30 = story + mini-quiz
FOR i IN 22..29 LOOP
UPDATE program.plan_steps SET module_code='STORY_SUCCESS_MINIQUIZ'
WHERE template_id=l1 AND day_no=i AND slot='16:30'::time AND (module_code IS NULL OR module_code='');
END LOOP;

  -- Ngày 30: 12:30 = kế hoạch phòng tái
UPDATE program.plan_steps SET module_code='RELAPSE_PREVENTION_PLAN'
WHERE template_id=l1 AND day_no=30 AND slot='12:30'::time AND (module_code IS NULL OR module_code='');

-- Bước bị thiếu ở L1 ngày 30 lúc 16:30 (đảm bảo đủ 120 bước)
INSERT INTO program.plan_steps(id, template_id, day_no, slot, title, details, max_minutes, created_at, module_code)
SELECT gen_random_uuid(), l1, 30, '16:30'::time,
    'Lễ huy hiệu + chia sẻ thành tựu', 'Nhìn lại 3 bài học rút ra', 10, now(), 'STORY_SUCCESS_MINIQUIZ'
    WHERE NOT EXISTS (
    SELECT 1 FROM program.plan_steps WHERE template_id=l1 AND day_no=30 AND slot='16:30'::time
  );
END $$;

-- 6) Verify nhanh
-- 6.1 Đủ 4 bước mỗi ngày cho L1?
WITH t AS (
    SELECT total_days FROM program.plan_templates WHERE id='11111111-1111-1111-1111-111111111111'
)
SELECT 'L1_30D' AS code,
       (SELECT total_days FROM t) AS total_days,
       (SELECT total_days*4 FROM t) AS expected_steps_4perday,
       (SELECT COUNT(*) FROM program.plan_steps WHERE template_id='11111111-1111-1111-1111-111111111111') AS actual_steps,
       ((SELECT COUNT(*) FROM program.plan_steps WHERE template_id='11111111-1111-1111-1111-111111111111')
           = (SELECT total_days*4 FROM t)) AS ok;

-- 6.2 Bước nào còn chưa có module_code?
SELECT day_no, slot, title
FROM program.plan_steps
WHERE template_id='11111111-1111-1111-1111-111111111111' AND (module_code IS NULL OR module_code='')
ORDER BY day_no, slot;

-- 6.3 Thống kê theo module_code
SELECT module_code, COUNT(*) cnt
FROM program.plan_steps
WHERE template_id='11111111-1111-1111-1111-111111111111'
GROUP BY module_code
ORDER BY cnt DESC NULLS LAST;

COMMIT;

-- (Tuỳ chọn) Dọn helper
-- DROP FUNCTION IF EXISTS program.put_module(text,text,text,int,jsonb);

;

-- ===================================================
-- SOURCE FILE: V22__smoke_events_drop_occurred_at.sql
-- ===================================================
-- V22: keep columns, re-define the view safely
create or replace view program.v_recent_smoke_events as
select
    id,
    program_id,
    user_id,
    event_at as occurred_at,     -- giữ tên legacy
    kind,
    puffs,
    cigarettes,
    reason,
    repair_action,
    repaired,
    created_at,
    event_type,
    event_at,                    -- giữ nguyên cột event_at
    note,
    age(now(), event_at) as ago  -- nếu UI cần text thì dùng ::text
from program.smoke_events;

;

-- ===================================================
-- SOURCE FILE: V23__patch_v_recent_smoke_events_keep_columns.sql
-- ===================================================
-- V23__patch_v_recent_smoke_events_keep_columns.sql
create or replace view program.v_recent_smoke_events as
select
    id,
    program_id,
    user_id,
    -- Giữ tên legacy 'occurred_at' nhưng lấy từ event_at chuẩn mới
    event_at as occurred_at,
    kind,
    puffs,
    cigarettes,
    reason,
    repair_action,
    repaired,
    created_at,
    event_type,
    event_at,                 -- giữ lại cột này để không "drop column"
    note,
    age(now(), event_at) as ago  -- nếu trước đây 'ago' là interval
from program.smoke_events;

;

-- ===================================================
-- SOURCE FILE: V24__quiz_personalization.sql
-- ===================================================
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

;

-- ===================================================
-- SOURCE FILE: V25__fix_choice_label_columns.sql
-- ===================================================
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

;

-- ===================================================
-- SOURCE FILE: V26__add_quiz_assignment_origin_manual.sql
-- ===================================================
-- Thêm 'MANUAL' vào type (chỉ THÊM, chưa dùng ngay)
do $$
begin
  if not exists (
    select 1
    from pg_enum e
    join pg_type t on t.oid = e.enumtypid
    join pg_namespace n on n.oid = t.typnamespace
    where n.nspname = 'program'
      and t.typname = 'quiz_assignment_origin'
      and e.enumlabel = 'MANUAL'
  ) then
alter type program.quiz_assignment_origin add value 'MANUAL';
end if;
end $$;

;

-- ===================================================
-- SOURCE FILE: V27__use_manual_default_and_backfill.sql
-- ===================================================
-- Đặt default và backfill sau khi V26 đã COMMIT
alter table program.quiz_assignments
    alter column origin set default 'MANUAL';

update program.quiz_assignments
set origin = 'MANUAL'
where origin is null;

;

-- ===================================================
-- SOURCE FILE: V28__seed_daily_plan_templates_levels_1_2_3.sql
-- ===================================================
-- ===========================
-- V28: add level column + seed plan templates (L1=30d, L2=45d, L3=60d)
-- Schema: program
-- ===========================

-- Cần cho gen_random_uuid()
create extension if not exists pgcrypto;

-- Bổ sung cột level cho plan_templates (cho cả DB cũ và DB mới)
alter table program.plan_templates
    add column if not exists level int;

-- Helper TABLE plan_steps đã có từ migration cũ, không tạo lại ở đây
-- (không dùng create table if not exists nữa)

-- Helper function (gọi trong DO, drop ở cuối)
create or replace function program.add_step(
  tid uuid, d int, hhmm text, t text, det text, mm int
) returns void
language plpgsql as $fn$
begin
insert into program.plan_steps(id, template_id, day_no, slot, title, details, max_minutes)
values (gen_random_uuid(), tid, d, (hhmm)::time, t, det, mm);
end
$fn$;

do $$
declare
l1 uuid := '11111111-1111-1111-1111-111111111111'; -- L1: Thức Tỉnh (30d)
  l2 uuid := '22222222-2222-2222-2222-222222222222'; -- L2: Thay Đổi (45d)
  l3 uuid := '33333333-3333-3333-3333-333333333333'; -- L3: Tự Do (60d)
begin
insert into program.plan_templates(id, level, code, name, total_days)
values
    (l1, 1, 'L1_30D', 'THỨC TỈNH (30 ngày)', 30),
    (l2, 2, 'L2_45D', 'THAY ĐỔI (45 ngày)', 45),
    (l3, 3, 'L3_60D', 'TỰ DO (60 ngày)', 60)
    on conflict (id) do nothing;

-- ... toàn bộ phần perform program.add_step(...) giữ nguyên như bạn đã viết ...
end $$;

drop function if exists program.add_step(uuid,int,text,text,text,int);

;

-- ===================================================
-- SOURCE FILE: V29__seed_plan_steps_levels_1_2_3.sql
-- ===================================================
-- V29: seed plan_steps cho 3 level (L1=30d, L2=45d, L3=60d)

create extension if not exists pgcrypto;

-- Helper function: thêm 1 step vào plan_steps
create or replace function program.add_step(
  tid uuid, d int, hhmm text, t text, det text, mm int
) returns void
language plpgsql as $fn$
begin
insert into program.plan_steps(id, template_id, day_no, slot, title, details, max_minutes)
values (gen_random_uuid(), tid, d, (hhmm)::time, t, det, mm);
end
$fn$;

do $seed$
declare
l1 uuid := '11111111-1111-1111-1111-111111111111'; -- L1: Thức Tỉnh (30d)
  l2 uuid := '22222222-2222-2222-2222-222222222222'; -- L2: Thay Đổi (45d)
  l3 uuid := '33333333-3333-3333-3333-333333333333'; -- L3: Tự Do (60d)
begin
  -- Đảm bảo 3 template tồn tại
insert into program.plan_templates(id, level, code, name, total_days)
values
    (l1, 1, 'L1_30D', 'THỨC TỈNH (30 ngày)', 30),
    (l2, 2, 'L2_45D', 'THAY ĐỔI (45 ngày)', 45),
    (l3, 3, 'L3_60D', 'TỰ DO (60 ngày)', 60)
    on conflict (id) do nothing;

-- =========================
-- L1 — THỨC TỈNH (30 ngày)
-- =========================

-- Ngày 1
perform program.add_step(l1,1,'07:30','Check-in sáng + chọn mục tiêu 7 ngày',
      'Kéo thang thèm/stress; chọn giấc ngủ; gợi ý: Thở 3’, Uống nước, Đi bộ 3’',10);
  perform program.add_step(l1,1,'12:30','Học 4D + tạo Urge Log thử',
      'Chọn nguyên nhân & mức độ; chạy Timer 3’ để thực hành 4D',7);
  perform program.add_step(l1,1,'16:30','Lợi ích sau 24h bỏ thuốc (mini-slide + quiz)',
      '3 slide + 3 câu hỏi',5);
  perform program.add_step(l1,1,'21:00','Nhật ký 2 câu + chọn Mantra',
      'Viết 1–2 câu; chọn câu động lực (có giọng đọc)',12);

  -- Ngày 2
  perform program.add_step(l1,2,'07:30','Checklist dọn môi trường + nhắn người thân',
      'Bỏ bật lửa/tàn thuốc; giặt áo ám mùi; nhắn người thân',12);
  perform program.add_step(l1,2,'12:30','Urge Log + Timer 3’ (4D)',
      'Chọn nguyên nhân/mức độ; vượt cơn 3’',8);
  perform program.add_step(l1,2,'16:30','CBT nhanh: thay câu nghĩ “một điếu không sao”',
      'Điền A–B–C và câu thay thế thực tế hơn',12);
  perform program.add_step(l1,2,'21:00','Tổng kết + Mantra','Viết ngắn; đặt câu cho ngày mai',12);

  -- Ngày 3
  perform program.add_step(l1,3,'07:30','Check-in + thở hướng dẫn 5’','Nếu thèm ≥7, nghe thở 5’',12);
  perform program.add_step(l1,3,'12:30','Kịch bản xã hội','Cà phê/đồng nghiệp/một mình – chọn câu từ chối',10);
  perform program.add_step(l1,3,'16:30','Mindfulness body-scan 5’','Nghe hướng dẫn, ghi 1 câu cảm nhận',12);
  perform program.add_step(l1,3,'21:00','Tổng kết + Mantra',null,12);

  -- Ngày 4
  perform program.add_step(l1,4,'07:30','Heatmap giờ hay thèm + áp lịch nhắc','Chọn 2–3 khung giờ nhắc',10);
  perform program.add_step(l1,4,'12:30','Chọn vật thay thế tay–miệng 24h','Kẹo the/tăm/đồ bóp tay',8);
  perform program.add_step(l1,4,'16:30','1 phiên Pomodoro 25–5 không thuốc',null,12);
  perform program.add_step(l1,4,'21:00','Tổng kết + Mantra',null,12);

  -- Ngày 5
  perform program.add_step(l1,5,'07:30','Tuyên ngôn “mình là người không hút” + 1 hành động',
      'Ví dụ: 2 chai nước / đi bộ 10’',10);
  perform program.add_step(l1,5,'12:30','Urge Log + mini-quiz tim mạch','2 câu',10);
  perform program.add_step(l1,5,'16:30','Tập nói “không” (nghe mẫu + chọn câu)',null,12);
  perform program.add_step(l1,5,'21:00','Tổng kết + Mantra',null,12);

  -- Ngày 6
  perform program.add_step(l1,6,'07:30','Slip? Flow phục hồi','Chọn lý do và 1 hành động sửa; không reset toàn bộ streak',12);
  perform program.add_step(l1,6,'12:30','Kế hoạch cuối tuần không khói',null,10);
  perform program.add_step(l1,6,'16:30','Mindfulness 5’ + động viên',null,12);
  perform program.add_step(l1,6,'21:00','Tổng kết + Mantra',null,12);

  -- Ngày 7
  perform program.add_step(l1,7,'07:30','Khảo sát 7 ngày + Check-in',null,12);
  perform program.add_step(l1,7,'12:30','Rút kinh nghiệm: Top-3 chiến lược',null,10);
  perform program.add_step(l1,7,'16:30','Chọn phần thưởng nhỏ',null,10);
  perform program.add_step(l1,7,'21:00','Kế hoạch tuần sau','Chọn trọng tâm (ngủ/xã hội/Pomodoro...)',12);

  -- Ngày 8..14
  perform program.add_step(l1,8,'07:30','Check-in sáng',null,10);
  perform program.add_step(l1,8,'12:30','Urge Log',null,8);
  perform program.add_step(l1,8,'16:30','Bộ giảm stress 10’','Thở, giãn cơ, đi bộ ngắn',12);
  perform program.add_step(l1,8,'21:00','Journal + Mantra',null,12);

  perform program.add_step(l1,9,'07:30','Check-in sáng',null,10);
  perform program.add_step(l1,9,'12:30','Urge Log',null,8);
  perform program.add_step(l1,9,'16:30','5 thói quen ngủ','Giảm màn hình; tránh cafe sau 15h; phòng mát/thoáng',10);
  perform program.add_step(l1,9,'21:00','Journal + Mantra',null,12);

  perform program.add_step(l1,10,'07:30','Check-in sáng',null,10);
  perform program.add_step(l1,10,'12:30','Urge Log',null,8);
  perform program.add_step(l1,10,'16:30','Đi bộ/giãn cơ 10’','Đặt timer; hướng 1000 bước',12);
  perform program.add_step(l1,10,'21:00','Journal + Mantra',null,12);

  perform program.add_step(l1,11,'07:30','Check-in sáng',null,10);
  perform program.add_step(l1,11,'12:30','Kịch bản xã hội','Chọn tình huống + câu nói',12);
  perform program.add_step(l1,11,'16:30','Urge Log',null,8);
  perform program.add_step(l1,11,'21:00','Journal + Mantra',null,12);

  perform program.add_step(l1,12,'07:30','Check-in sáng',null,10);
  perform program.add_step(l1,12,'12:30','Urge Log',null,8);
  perform program.add_step(l1,12,'16:30','Pomodoro 25–5',null,12);
  perform program.add_step(l1,12,'21:00','Journal + Mantra',null,12);

  perform program.add_step(l1,13,'07:30','Khẳng định bản sắc + 1 hành động',null,10);
  perform program.add_step(l1,13,'12:30','Urge Log',null,8);
  perform program.add_step(l1,13,'16:30','Từ chối khéo','Nghe mẫu + chọn câu',12);
  perform program.add_step(l1,13,'21:00','Journal + Mantra',null,12);

  perform program.add_step(l1,14,'07:30','Check-in tổng tuần',null,10);
  perform program.add_step(l1,14,'12:30','Top-3 chiến lược hiệu quả',null,10);
  perform program.add_step(l1,14,'16:30','Phần thưởng nhỏ',null,10);
  perform program.add_step(l1,14,'21:00','Chọn trọng tâm tuần tới',null,12);

  -- Ngày 15..21 (loop)
for i in 15..21 loop
      perform program.add_step(l1,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l1,i,'12:30','Tình huống rủi ro cao','Chọn kịch bản khó + phương án ứng phó',12);
      perform program.add_step(l1,i,'16:30','Theo dõi thói quen thay thế','Đánh dấu uống nước/đi bộ/giãn cơ',12);
      perform program.add_step(l1,i,'21:00','Journal + Mantra',null,12);
end loop;

  -- Ngày 22..29 (loop)
for i in 22..29 loop
      perform program.add_step(l1,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l1,i,'12:30','Cập nhật tiền tiết kiệm','Nhập số tiền không chi cho thuốc hôm nay',8);
      perform program.add_step(l1,i,'16:30','Câu chuyện thành công + mini-quiz','Đọc 1 câu chuyện; trả lời 2 câu',10);
      perform program.add_step(l1,i,'21:00','Journal + Mantra',null,12);
end loop;

  -- Ngày 30
  perform program.add_step(l1,30,'07:30','Tổng kết tiến trình 30 ngày',
      'Xem huy hiệu/điểm/tiền tiết kiệm/heatmap',12);
  perform program.add_step(l1,30,'12:30','Cập nhật kế hoạch phòng tái','3 bẫy lớn + 3 hành động sẵn',12);
  perform program.add_step(l1,30,'21:00','Chọn lộ trình tiếp theo','Duy trì hoặc chuyển Level 2',12);

  -- =========================
  -- L2 — THAY ĐỔI (45 ngày)
  -- =========================
  perform program.add_step(l2,1,'07:30','Check-in + tư vấn NRT phối hợp',
      'Đánh dấu dùng miếng dán/gum (tham khảo bác sĩ)',12);
  perform program.add_step(l2,1,'12:30','Học 4D + log thử','Tạo Urge log, Timer 3’',8);
  perform program.add_step(l2,1,'16:30','CBT 1 tình huống thật','Điền A–B–C + câu thay thế',12);
  perform program.add_step(l2,1,'21:00','Nhật ký + Mantra',null,12);

  perform program.add_step(l2,2,'07:30','Chọn liều NRT tham khảo','Luôn hỏi bác sĩ khi cần',12);
  perform program.add_step(l2,2,'12:30','Urge + Timer 3’',null,10);
  perform program.add_step(l2,2,'16:30','Body-scan 5’',null,12);
  perform program.add_step(l2,2,'21:00','Nhật ký + an toàn NRT','Tick triệu chứng nhẹ nếu có',12);

  perform program.add_step(l2,3,'07:30','Check-in + thở 5’',null,12);
  perform program.add_step(l2,3,'12:30','Tình huống xã hội',null,12);
  perform program.add_step(l2,3,'16:30','Thay khung nhận thức',null,12);
  perform program.add_step(l2,3,'21:00','Nhật ký + safety NRT',null,12);

  perform program.add_step(l2,4,'07:30','Áp lịch nhắc cá nhân hoá',null,10);
  perform program.add_step(l2,4,'12:30','Vật thay thế 24h',null,8);
  perform program.add_step(l2,4,'16:30','Pomodoro 25–5',null,12);
  perform program.add_step(l2,4,'21:00','Nhật ký + Mantra',null,12);

  perform program.add_step(l2,5,'07:30','Tuyên ngôn hành động',null,10);
  perform program.add_step(l2,5,'12:30','Urge + Quiz 2 câu',null,10);
  perform program.add_step(l2,5,'16:30','Từ chối khéo',null,12);
  perform program.add_step(l2,5,'21:00','Nhật ký + Mantra',null,12);

  perform program.add_step(l2,6,'07:30','Flow phục hồi','Không reset toàn bộ streak',12);
  perform program.add_step(l2,6,'12:30','Kế hoạch cuối tuần',null,10);
  perform program.add_step(l2,6,'16:30','Mindfulness 5’',null,12);
  perform program.add_step(l2,6,'21:00','Nhật ký + safety',null,12);

  perform program.add_step(l2,7,'07:30','7-day PPA',null,12);
  perform program.add_step(l2,7,'12:30','Top-3 chiến lược',null,10);
  perform program.add_step(l2,7,'16:30','Phần thưởng nhỏ',null,10);
  perform program.add_step(l2,7,'21:00','Kế hoạch tuần 2',null,12);

  -- Ngày 8..21
for i in 8..21 loop
      perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l2,i,'12:30','Urge log','Timer 3’ nếu cần',8);
      perform program.add_step(l2,i,'16:30','Kỹ năng phù hợp','Ngủ/giảm stress/exercise/CBT',12);
      perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
end loop;

  -- Ngày 22..28
for i in 22..28 loop
      perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l2,i,'12:30','Luân phiên thói quen','Pomodoro/mindfulness/exercise/xã hội',12);
      perform program.add_step(l2,i,'16:30','Cập nhật tiền tiết kiệm',null,8);
      perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
end loop;
update program.plan_steps
set title='Tổng kết tuần + phần thưởng'
where template_id=l2 and day_no=28 and slot='21:00';

-- Ngày 29..35
for i in 29..35 loop
      perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l2,i,'12:30','Luân phiên thói quen',null,12);
      perform program.add_step(l2,i,'16:30','Cập nhật tiền tiết kiệm',null,8);
      perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
end loop;
update program.plan_steps set title='Tổng kết tuần + phần thưởng'
where template_id=l2 and day_no=35 and slot='21:00';

-- Ngày 36..42
for i in 36..42 loop
      perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l2,i,'12:30','Luân phiên thói quen',null,12);
      perform program.add_step(l2,i,'16:30','Cập nhật tiền tiết kiệm',null,8);
      perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
end loop;
update program.plan_steps set title='Tổng kết tuần + phần thưởng'
where template_id=l2 and day_no=42 and slot='21:00';

-- Ngày 43..45
for i in 43..45 loop
      perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l2,i,'12:30','Luân phiên thói quen',null,12);
      perform program.add_step(l2,i,'16:30','Cập nhật tiền tiết kiệm',null,8);
      perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
end loop;
update program.plan_steps set title='Tổng kết tuần + phần thưởng'
where template_id=l2 and day_no=45 and slot='21:00';

-- =========================
-- L3 — TỰ DO (60 ngày)
-- =========================

-- Ngày 1..3
perform program.add_step(l3,1,'07:30','Check-in nâng cao + tư vấn thuốc','Đánh dấu thuốc/NRT; tuỳ chọn đo mạch',15);
  perform program.add_step(l3,1,'12:30','Checklist môi trường + nhắn người thân',null,10);
  perform program.add_step(l3,1,'16:30','CBT-ABC (case nặng)',null,12);
  perform program.add_step(l3,1,'21:00','Nhật ký + Mantra',null,12);

  perform program.add_step(l3,2,'07:30','Kế hoạch thuốc','Giờ dán miếng dán; gum dự phòng',12);
  perform program.add_step(l3,2,'12:30','Urge baseline + đặt nhắc',null,10);
  perform program.add_step(l3,2,'16:30','Mindfulness 5’ + đi bộ 5’',null,12);
  perform program.add_step(l3,2,'21:00','Nhật ký + safety baseline',null,12);

  perform program.add_step(l3,3,'07:30','Check-in + thở 5’',null,12);
  perform program.add_step(l3,3,'12:30','Urge + Timer 3’ + gọi bạn hỗ trợ','Nếu thèm ≥8, bấm gọi buddy',10);
  perform program.add_step(l3,3,'16:30','CBT thay niềm tin lõi','Viết 1 câu thay thế mạnh',12);
  perform program.add_step(l3,3,'21:00','Nhật ký + kiểm tra tác dụng phụ',null,12);

  -- Ngày 4..14
for i in 4..14 loop
      perform program.add_step(l3,i,'07:30','Check-in sáng (nhắc dày tuần đầu)',null,12);
      perform program.add_step(l3,i,'12:30','Urge Log + 4D','Timer 3’',10);
      perform program.add_step(l3,i,'16:30','Luân phiên bài phù hợp','Thở/mindfulness/pomodoro/xã hội',12);
      perform program.add_step(l3,i,'21:00','Nhật ký + safety (nếu dùng thuốc)',null,12);
end loop;

  -- Ngày 15..28
for i in 15..28 loop
      perform program.add_step(l3,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l3,i,'12:30','Tình huống rủi ro cao','Chuẩn bị câu nói/động tác',12);
      perform program.add_step(l3,i,'16:30','Vận động nhẹ 10’','Đi bộ/giãn cơ',12);
      perform program.add_step(l3,i,'21:00','Nhật ký + Mantra',null,12);
end loop;

  -- Ngày 29..42
for i in 29..42 loop
      perform program.add_step(l3,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l3,i,'12:30','Kỹ năng xã hội nâng cao','Nói “không” khi đi nhậu/cà phê',12);
      perform program.add_step(l3,i,'16:30','Pomodoro 25–5',null,12);
      perform program.add_step(l3,i,'21:00','Nhật ký + Mantra',null,12);
end loop;

  -- Ngày 43..60
for i in 43..60 loop
      perform program.add_step(l3,i,'07:30','Check-in sáng',null,10);
      perform program.add_step(l3,i,'12:30','Kế hoạch phòng ngừa tái sử dụng','Chọn 1 bẫy + 1 hành động sẵn sàng',12);
      perform program.add_step(l3,i,'16:30','Củng cố bản sắc + mục tiêu thể lực','Đặt mục tiêu bước chân/đi bộ',12);
      perform program.add_step(l3,i,'21:00','Tổng kết tuần/huân chương','Nếu trùng mốc tuần thì trao huy hiệu',12);
end loop;

end
$seed$;

drop function if exists program.add_step(uuid,int,text,text,text,int);

;

-- ===================================================
-- SOURCE FILE: V30__add_template_info_to_programs.sql
-- ===================================================
-- Migration V30: Thêm template information vào programs table
-- Mục đích: Lưu thông tin plan template (code, name) để hiển thị dashboard
--
-- Flow:
-- 1. Khi customer start enrollment, program được tạo với template info
-- 2. MeService sử dụng template code/name để hiển thị trong dashboard
-- 3. Không cần join với plan_templates table

BEGIN;

-- Thêm columns cho plan template information
ALTER TABLE program.programs
ADD COLUMN IF NOT EXISTS plan_template_id UUID,
ADD COLUMN IF NOT EXISTS template_code VARCHAR(50),
ADD COLUMN IF NOT EXISTS template_name VARCHAR(255);

-- Tạo index cho plan_template_id
CREATE INDEX IF NOT EXISTS idx_programs_plan_template_id
ON program.programs(plan_template_id);

-- Tạo index cho template_code (có thể cần join theo code)
CREATE INDEX IF NOT EXISTS idx_programs_template_code
ON program.programs(template_code);

-- Foreign key constraint (optional - nếu muốn enforce referential integrity)
-- Nếu uncomment dòng dưới, hãy chắc chắn plan_templates table tồn tại
-- ALTER TABLE program.programs
-- ADD CONSTRAINT fk_programs_plan_template_id
-- FOREIGN KEY (plan_template_id)
-- REFERENCES program.plan_templates(id) ON DELETE SET NULL;

COMMIT;


;

-- ===================================================
-- SOURCE FILE: V31__plan_quiz_schedules.sql
-- ===================================================
-- V31: add plan_quiz_schedules + order_no for quiz_assignments

create table if not exists program.plan_quiz_schedules (
    id uuid primary key,
    plan_template_id uuid not null references program.plan_templates(id) on delete cascade,
    quiz_template_id uuid not null references program.quiz_templates(id) on delete cascade,
    start_offset_day int not null default 1,
    every_days int not null default 0,
    order_no int,
    active boolean not null default true,
    created_at timestamptz not null default now()
);

create index if not exists idx_plan_quiz_schedule_template
    on program.plan_quiz_schedules(plan_template_id, start_offset_day, order_no);

-- Add order_no to quiz_assignments to preserve schedule ordering
alter table program.quiz_assignments
    add column if not exists order_no int;

;

-- ===================================================
-- SOURCE FILE: V32__Add_Streak_Recovery_Feature.sql
-- ===================================================
-- V32: Add Streak Recovery Feature

-- 1. Thêm cột đếm số lần phục hồi vào bảng programs
ALTER TABLE program.programs
ADD COLUMN streak_recovery_used_count INT NOT NULL DEFAULT 0;

-- 2. Thêm các cột mới vào bảng step_assignments để hỗ trợ nhiệm vụ phục hồi
ALTER TABLE program.step_assignments
ADD COLUMN assignment_type VARCHAR(255) NOT NULL DEFAULT 'REGULAR',
ADD COLUMN streak_break_id UUID,
ADD COLUMN module_code VARCHAR(255),
ADD COLUMN module_version VARCHAR(255),
ADD COLUMN title_override VARCHAR(255);
-- 3. Tạo bảng cấu hình cho các module phục hồi
CREATE TABLE program.streak_recovery_configs (
    attempt_order INT PRIMARY KEY,
    module_code VARCHAR(255) NOT NULL
);

-- 4. (Tùy chọn) Thêm dữ liệu mẫu cho bảng cấu hình
-- Bạn cần đảm bảo các module với mã này đã tồn tại trong bảng content_modules
-- INSERT INTO program.streak_recovery_configs (attempt_order, module_code) VALUES
-- (1, 'RECOVERY_TASK_1'),
-- (2, 'RECOVERY_TASK_2'),
-- (3, 'RECOVERY_TASK_3');

;

-- ===================================================
-- SOURCE FILE: V33__Seed_Streak_Recovery_Configs.sql
-- ===================================================
-- V33: Chèn dữ liệu cấu hình ban đầu cho tính năng phục hồi streak.
-- File này đảm bảo rằng hệ thống luôn có các module được định nghĩa cho 3 lần phục hồi đầu tiên.

INSERT INTO program.streak_recovery_configs (attempt_order, module_code) VALUES
(1, 'RECOVERY_TASK_1'),
(2, 'RECOVERY_TASK_2'),
(3, 'RECOVERY_TASK_3');

;

-- ===================================================
-- SOURCE FILE: V34__cleanup_duplicate_plan_steps.sql
-- ===================================================
-- V34: Cleanup duplicate plan steps caused by V28 and V29
-- This script removes duplicate entries in program.plan_steps for the seeded templates,
-- keeping only one instance for each unique step.

WITH duplicates AS (
    SELECT
        id,
        ROW_NUMBER() OVER(
            PARTITION BY template_id, day_no, slot, title
            ORDER BY id
        ) AS rn
    FROM
        program.plan_steps
    WHERE
        template_id IN (
            '11111111-1111-1111-1111-111111111111',
            '22222222-2222-2222-2222-222222222222',
            '33333333-3333-3333-3333-333333333333'
        )
)
DELETE FROM
    program.plan_steps
WHERE
    id IN (SELECT id FROM duplicates WHERE rn > 1);

;

-- ===================================================
-- SOURCE FILE: V35__add_code_to_quiz_templates.sql
-- ===================================================
-- V35: Add 'code' column to quiz_templates table for recovery quizzes
ALTER TABLE program.quiz_templates
ADD COLUMN IF NOT EXISTS code VARCHAR(255);

-- Add a unique constraint to ensure codes are not duplicated
-- Note: This might fail if you have existing duplicate data (e.g., all NULLs).
-- For a new column, this should be safe.
ALTER TABLE program.quiz_templates
ADD CONSTRAINT uq_quiz_template_code UNIQUE (code);

;

-- ===================================================
-- SOURCE FILE: V36__Update_streak_recovery_configs_to_use_quizzes.sql
-- ===================================================
-- V36: Cập nhật cấu hình phục hồi streak để sử dụng QUIZ cho cả 3 lần thử.
-- File này sẽ ghi đè lên dữ liệu đã được seed bởi V33.

-- Xóa toàn bộ cấu hình cũ để đảm bảo tính nhất quán và có thể chạy lại an toàn.
DELETE FROM program.streak_recovery_configs;

-- Chèn lại cấu hình mới, trong đó cả 3 lần đều là QUIZ.
INSERT INTO program.streak_recovery_configs (attempt_order, module_code) VALUES
( 1, 'RECOVERY_QUIZ_1'),
(2, 'RECOVERY_QUIZ_2'),
(3, 'RECOVERY_QUIZ_3');

;

-- ===================================================
-- SOURCE FILE: V37__add_streak_recovery_to_quiz_origin_enum.sql
-- ===================================================
-- V37: Add STREAK_RECOVERY value to the quiz_assignment_origin enum type
ALTER TYPE program.quiz_assignment_origin ADD VALUE IF NOT EXISTS 'STREAK_RECOVERY';

;

-- ===================================================
-- SOURCE FILE: V38__create_user_baseline_results.sql
-- ===================================================
-- V38: Table lưu kết quả quiz onboarding (baseline) theo user

create table if not exists program.user_baseline_results (
    id uuid primary key,
    user_id uuid not null,
    quiz_template_id uuid not null,
    total_score integer not null,
    severity program.severity_level not null,
    created_at timestamptz not null default now()
);

create unique index if not exists uq_user_baseline_user on program.user_baseline_results(user_id);
create index if not exists idx_user_baseline_template on program.user_baseline_results(quiz_template_id);

;

-- ===================================================
-- SOURCE FILE: V40__create_badge_system.sql
-- ===================================================
-- 1. Thêm cột has_paused vào programs
ALTER TABLE program.programs ADD COLUMN IF NOT EXISTS has_paused BOOLEAN DEFAULT FALSE;

-- 2. Tạo bảng badges
CREATE TABLE program.badges (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE, -- PROG_LV1, STREAK_LV2...
    category VARCHAR(20) NOT NULL, -- PROGRAM, STREAK, QUIZ
    level INT NOT NULL, -- 1, 2, 3
    name VARCHAR(255) NOT NULL,
    description TEXT,
    icon_url VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. Tạo bảng user_badges
CREATE TABLE program.user_badges (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    badge_id UUID NOT NULL REFERENCES program.badges(id),
    program_id UUID REFERENCES program.programs(id),
    earned_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uk_user_badge_program UNIQUE (user_id, badge_id, program_id) -- Một user chỉ nhận 1 loại huy hiệu 1 lần cho 1 program
);

-- 4. Seed Data
-- Program Badges
INSERT INTO program.badges (id, code, category, level, name, description, icon_url) VALUES
('11111111-1111-1111-1111-111111111111', 'PROG_LV1', 'PROGRAM', 1, 'Khởi Hành', 'Bắt đầu hành trình cai thuốc lá.', 'assets/badges/prog_lv1.png'),
('11111111-1111-1111-1111-111111111112', 'PROG_LV2', 'PROGRAM', 2, 'Kiên Trì', 'Đi được một nửa chặng đường mà không tạm dừng.', 'assets/badges/prog_lv2.png'),
('11111111-1111-1111-1111-111111111113', 'PROG_LV3', 'PROGRAM', 3, 'Về Đích', 'Hoàn thành toàn bộ lộ trình cai thuốc.', 'assets/badges/prog_lv3.png');

-- Streak Badges
INSERT INTO program.badges (id, code, category, level, name, description, icon_url) VALUES
('22222222-2222-2222-2222-222222222221', 'STREAK_LV1', 'STREAK', 1, 'Tuần Lễ Vàng', 'Đạt chuỗi 7 ngày không hút thuốc.', 'assets/badges/streak_lv1.png'),
('22222222-2222-2222-2222-222222222222', 'STREAK_LV2', 'STREAK', 2, 'Thói Quen Mới', 'Đạt chuỗi ngày bằng một nửa lộ trình.', 'assets/badges/streak_lv2.png'),
('22222222-2222-2222-2222-222222222223', 'STREAK_LV3', 'STREAK', 3, 'Chiến Binh Tự Do', 'Giữ vững chuỗi không hút thuốc suốt cả lộ trình.', 'assets/badges/streak_lv3.png');

-- Quiz Badges
INSERT INTO program.badges (id, code, category, level, name, description, icon_url) VALUES
('33333333-3333-3333-3333-333333333331', 'QUIZ_LV1', 'QUIZ', 1, 'Tự Nhận Thức', 'Hoàn thành bài kiểm tra định kỳ đầu tiên.', 'assets/badges/quiz_lv1.png'),
('33333333-3333-3333-3333-333333333332', 'QUIZ_LV2', 'QUIZ', 2, 'Tiến Triển Tốt', 'Có kết quả kiểm tra cải thiện hoặc ổn định 2 lần liên tiếp.', 'assets/badges/quiz_lv2.png'),
('33333333-3333-3333-3333-333333333333', 'QUIZ_LV3', 'QUIZ', 3, 'Làm Chủ', 'Hoàn thành tất cả bài kiểm tra với mức độ phụ thuộc thấp.', 'assets/badges/quiz_lv3.png');

;

-- ===================================================
-- SOURCE FILE: V41__drop_reason_from_streak_breaks.sql
-- ===================================================
-- V19__drop_reason_from_streak_breaks.sql (tuỳ chọn)
ALTER TABLE program.streak_breaks DROP COLUMN IF EXISTS reason;

;

-- ===================================================
-- SOURCE FILE: R__ensure_program_schema.sql
-- ===================================================
-- R__ensure_program_schema.sql
-- Safe/idempotent: có thể chạy nhiều lần trên cùng DB.

-----------------------------
-- SCHEMA & OWNERSHIP
-----------------------------
do $$
begin
  if not exists (select 1 from pg_namespace where nspname = 'program') then
    execute 'create schema program';
end if;

begin
execute 'alter schema program owner to program_app_rw';
exception when insufficient_privilege then
    null;
end;
end $$;

-----------------------------
-- ENUMS (chỉ các enum còn dùng trong DB)
-----------------------------
do $$
begin
  if not exists (
    select 1 from pg_type t join pg_namespace n on n.oid=t.typnamespace
    where n.nspname='program' and t.typname='quiz_template_status'
  ) then
create type program.quiz_template_status as enum ('DRAFT','PUBLISHED','ARCHIVED');
end if;

  if not exists (
    select 1 from pg_type t join pg_namespace n on n.oid=t.typnamespace
    where n.nspname='program' and t.typname='attempt_status'
  ) then
create type program.attempt_status as enum ('OPEN','SUBMITTED');
end if;

  if not exists (
    select 1 from pg_type t join pg_namespace n on n.oid=t.typnamespace
    where n.nspname='program' and t.typname='severity_level'
  ) then
create type program.severity_level as enum ('LOW','MODERATE','HIGH','VERY_HIGH');
end if;

  -- enum quiz_scope có thể tồn tại ở môi trường cũ; KHÔNG bắt buộc tạo mới.
  -- Nếu cần tương thích, bỏ comment khối dưới:
  -- if not exists (
  --   select 1 from pg_type t join pg_namespace n on n.oid=t.typnamespace
  --   where n.nspname='program' and t.typname='quiz_scope'
  -- ) then
  --   create type program.quiz_scope as enum ('system','coach');
  -- end if;
end $$;

-----------------------------
-- programs (bảng gốc)
-- (Entity Program có startDate DATE NOT NULL và nhiều cột phụ)
-----------------------------
create table if not exists program.programs (
                                                id            uuid primary key,
                                                user_id       uuid not null,
                                                plan_days     int  not null,
                                                status        text not null,
                                                started_at    timestamptz,
                                                completed_at  timestamptz,
                                                created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    deleted_at    timestamptz
    );

-- Bổ sung cột còn thiếu cho programs
do $$
begin
  -- chatroom_id
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='chatroom_id'
  ) then
alter table program.programs add column chatroom_id uuid;
end if;

  -- coach_id
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='coach_id'
  ) then
alter table program.programs add column coach_id uuid;
end if;

  -- current_day (mặc định 1, NOT NULL)
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='current_day'
  ) then
alter table program.programs add column current_day int;
alter table program.programs alter column current_day set default 1;
update program.programs set current_day = 1 where current_day is null;
alter table program.programs alter column current_day set not null;
end if;

  -- total_score
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='total_score'
  ) then
alter table program.programs add column total_score int;
end if;

  -- entitlement_tier_at_creation
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='entitlement_tier_at_creation'
  ) then
alter table program.programs add column entitlement_tier_at_creation text;
end if;

  -- trial_started_at
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='trial_started_at'
  ) then
alter table program.programs add column trial_started_at timestamptz;
end if;

  -- trial_end_expected
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='trial_end_expected'
  ) then
alter table program.programs add column trial_end_expected timestamptz;
end if;

  -- severity (enum)
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='severity'
  ) then
alter table program.programs add column severity program.severity_level;
end if;

  -- start_date (LocalDate) + backfill từ started_at
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='start_date'
  ) then
alter table program.programs add column start_date date;
update program.programs
set start_date = (started_at at time zone 'UTC')::date
where start_date is null and started_at is not null;
-- nếu vẫn null (hàng cũ), set tạm ngày hôm nay UTC
update program.programs
set start_date = (now() at time zone 'UTC')::date
where start_date is null;
alter table program.programs alter column start_date set not null;
end if;
end $$;

-- FK chatroom_id nếu có bảng chatrooms
do $$
begin
  if exists (
    select 1 from information_schema.tables
    where table_schema='program' and table_name='chatrooms'
  ) then
begin
alter table program.programs
    add constraint fk_program_chatroom
        foreign key (chatroom_id) references program.chatrooms(id);
exception when duplicate_object then null;
end;
end if;
end $$;

-----------------------------
-- quiz_templates & liên quan
-----------------------------
create table if not exists program.quiz_templates(
                                                     id            uuid primary key,
                                                     name          text not null,
                                                     version       int not null default 1,
                                                     status        program.quiz_template_status not null default 'DRAFT',
                                                     language_code text,
                                                     published_at  timestamptz,
                                                     archived_at   timestamptz,
                                                     scope         text not null default 'system',  -- dùng TEXT (không còn enum)
                                                     owner_id      uuid,
                                                     created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now()
    );

-- Thêm cột còn thiếu / chuyển kiểu scope enum -> text
do $$
declare
v_udt text;
begin
  -- owner_id (đề phòng bảng cũ chưa có)
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='quiz_templates' and column_name='owner_id'
  ) then
alter table program.quiz_templates add column owner_id uuid;
end if;

  -- scope: nếu đang là enum program.quiz_scope thì chuyển sang text
select udt_name into v_udt
from information_schema.columns
where table_schema='program' and table_name='quiz_templates' and column_name='scope';

if v_udt = 'quiz_scope' then
    -- bỏ default trước để cắt phụ thuộc
begin
alter table program.quiz_templates alter column scope drop default;
exception when others then null; end;

alter table program.quiz_templates
alter column scope type text using scope::text;
end if;

  -- đặt default TEXT
begin
alter table program.quiz_templates alter column scope set default 'system';
exception when others then null; end;
end $$;

create table if not exists program.quiz_template_questions(
                                                              template_id uuid not null references program.quiz_templates(id) on delete cascade,
    question_no int not null,
    text        text not null,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    primary key (template_id, question_no)
    );

create table if not exists program.quiz_choice_labels(
                                                         template_id uuid not null,
                                                         question_no int not null,
                                                         score       int not null,
                                                         label       text not null,
                                                         primary key (template_id, question_no, score),
    foreign key (template_id, question_no)
    references program.quiz_template_questions(template_id, question_no)
    on delete cascade
    );

-- Unique constraint theo entity (@UniqueConstraint)
do $$
begin
  if not exists (
    select 1
    from pg_constraint c
    join pg_class     t on t.oid = c.conrelid
    join pg_namespace n on n.oid = t.relnamespace
    where n.nspname='program'
      and t.relname='quiz_templates'
      and c.conname='uq_quiz_template_name_scope_owner_version'
  ) then
alter table program.quiz_templates
    add constraint uq_quiz_template_name_scope_owner_version
        unique (name, scope, owner_id, version);
end if;
end $$;

-- Index theo entity (@Index scope, owner_id), chỉ tạo khi 2 cột đều tồn tại
do $$
begin
  if exists (
    select 1
    from information_schema.columns
    where table_schema='program' and table_name='quiz_templates' and column_name='scope'
  ) and exists (
    select 1
    from information_schema.columns
    where table_schema='program' and table_name='quiz_templates' and column_name='owner_id'
  ) then
create index if not exists idx_quiz_template_scope_owner
    on program.quiz_templates(scope, owner_id);
end if;
end $$;

-----------------------------
-- quiz_assignments
-----------------------------
create table if not exists program.quiz_assignments(
                                                       id                 uuid primary key,
                                                       template_id        uuid not null references program.quiz_templates(id),
    program_id         uuid not null references program.programs(id) on delete cascade,
    assigned_by_user_id uuid,
    period_days        int,
    start_offset_day   int,
    use_latest_version boolean default true,
    active             boolean default true,
    created_at         timestamptz not null default now(),
    created_by         uuid,
    every_days         int not null default 5,
    scope              text not null default 'system'   -- TEXT (không còn enum)
    );

-- Bổ sung cột còn thiếu / chuyển kiểu scope enum -> text
do $$
declare v_udt text;
begin
  -- đảm bảo cột every_days
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='quiz_assignments' and column_name='every_days'
  ) then
alter table program.quiz_assignments add column every_days int;
alter table program.quiz_assignments alter column every_days set default 5;
update program.quiz_assignments set every_days = 5 where every_days is null;
alter table program.quiz_assignments alter column every_days set not null;
end if;

  -- created_by
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='quiz_assignments' and column_name='created_by'
  ) then
alter table program.quiz_assignments add column created_by uuid;
end if;

  -- scope: nếu chưa có thì thêm; nếu là enum thì chuyển type
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='quiz_assignments' and column_name='scope'
  ) then
alter table program.quiz_assignments add column scope text;
alter table program.quiz_assignments alter column scope set default 'system';
update program.quiz_assignments set scope='system' where scope is null;
alter table program.quiz_assignments alter column scope set not null;
else
select udt_name into v_udt
from information_schema.columns
where table_schema='program' and table_name='quiz_assignments' and column_name='scope';

if v_udt = 'quiz_scope' then
begin
alter table program.quiz_assignments alter column scope drop default;
exception when others then null; end;

alter table program.quiz_assignments
alter column scope type text using scope::text;

begin
alter table program.quiz_assignments alter column scope set default 'system';
exception when others then null; end;
end if;
end if;
end $$;

create index if not exists idx_qas_program on program.quiz_assignments(program_id);

-----------------------------
-- quiz_attempts
-----------------------------
create table if not exists program.quiz_attempts(
                                                    id           uuid primary key,
                                                    program_id   uuid not null references program.programs(id) on delete cascade,
    template_id  uuid not null references program.quiz_templates(id),
    user_id      uuid not null,
    opened_at    timestamptz not null,
    submitted_at timestamptz,
    status       program.attempt_status not null default 'OPEN'
    );
create index if not exists idx_qatt_program_template
    on program.quiz_attempts(program_id, template_id, status);

-----------------------------
-- quiz_answers (PK (attempt_id, question_no))
-----------------------------
do $$
begin
  if not exists (
    select 1 from information_schema.tables
    where table_schema='program' and table_name='quiz_answers'
  ) then
create table program.quiz_answers(
                                     attempt_id  uuid not null,
                                     question_no int  not null,
                                     answer      integer,
                                     created_at  timestamptz not null default now(),
                                     primary key (attempt_id, question_no),
                                     constraint fk_qans_attempt foreign key (attempt_id)
                                         references program.quiz_attempts(id) on delete cascade
);
else
    -- ensure attempt_id
    if not exists (
      select 1 from information_schema.columns
      where table_schema='program' and table_name='quiz_answers' and column_name='attempt_id'
    ) then
alter table program.quiz_answers add column attempt_id uuid;
end if;

    -- ensure PK
    if not exists (
      select 1
      from pg_constraint c
      join pg_class t on t.oid = c.conrelid
      join pg_namespace n on n.oid = t.relnamespace
      where n.nspname='program' and t.relname='quiz_answers'
        and c.contype='p'
    ) then
begin
alter table program.quiz_answers add primary key (attempt_id, question_no);
exception when duplicate_table then null; end;
end if;

    -- ensure FK
begin
alter table program.quiz_answers
    add constraint fk_qans_attempt foreign key (attempt_id)
        references program.quiz_attempts(id) on delete cascade;
exception when duplicate_object then null; end;

    -- ensure answer is integer
    if exists (
      select 1 from information_schema.columns
      where table_schema='program' and table_name='quiz_answers'
        and column_name='answer' and data_type not in ('smallint','integer','bigint')
    ) then
begin
alter table program.quiz_answers
alter column answer type integer using nullif(trim(answer),'')::integer;
exception when others then
alter table program.quiz_answers rename column answer to answer_text;
alter table program.quiz_answers add column answer integer;
update program.quiz_answers
set answer = nullif(answer_text,'')::integer
where answer_text ~ '^\s*\d+\s*$';
end;
end if;

    -- ensure created_at
    if not exists (
      select 1 from information_schema.columns
      where table_schema='program' and table_name='quiz_answers' and column_name='created_at'
    ) then
alter table program.quiz_answers add column created_at timestamptz default now();
end if;
end if;
end $$;

-----------------------------
-- quiz_results
-----------------------------
create table if not exists program.quiz_results(
                                                   id           uuid primary key,
                                                   program_id   uuid not null references program.programs(id) on delete cascade,
    template_id  uuid not null references program.quiz_templates(id),
    quiz_version int not null,
    total_score  int not null,
    severity     program.severity_level not null,
    created_at   timestamptz not null default now()
    );
create index if not exists idx_qres_program_template
    on program.quiz_results(program_id, template_id, created_at desc);

-----------------------------
-- TRY DROP enum quiz_scope nếu không còn dùng
-----------------------------
do $$
begin
  if exists (
    select 1 from pg_type t
    join pg_namespace n on n.oid = t.typnamespace
    where n.nspname='program' and t.typname='quiz_scope'
  ) then
    -- chỉ drop khi KHÔNG còn cột dùng udt_name='quiz_scope'
    if not exists (
      select 1
      from information_schema.columns
      where table_schema='program' and udt_name='quiz_scope'
    ) then
      execute 'drop type program.quiz_scope';
end if;
end if;
end $$;

-----------------------------
-- GRANTS (tùy quyền của user đang chạy)
-----------------------------
do $$
begin
begin
execute 'grant usage on schema program to program_app_rw';
execute 'grant select, insert, update, delete on all tables in schema program to program_app_rw';
execute 'grant usage, select, update on all sequences in schema program to program_app_rw';
execute 'alter default privileges in schema program grant select, insert, update, delete on tables to program_app_rw';
execute 'alter default privileges in schema program grant usage, select, update on sequences to program_app_rw';
exception when insufficient_privilege then
    null;
end;
end $$;

;

-- ===================================================
-- SOURCE FILE: R__helper_views_streaks.sql
-- ===================================================
-- ===========================================
-- R__helper_views_streaks.sql
-- Repeatable: các view hỗ trợ giám sát streak & sự kiện gần đây
-- ===========================================

CREATE OR REPLACE VIEW program.v_program_streaks AS
SELECT
    p.id,
    p.user_id,
    p.plan_days,
    p.status,
    p.current_streak_days,
    p.longest_streak_days,
    p.last_smoke_at,
    p.slip_count,
    (now() - p.last_smoke_at) AS since_last_smoke
FROM program.programs p;

CREATE OR REPLACE VIEW program.v_recent_smoke_events AS
SELECT
    se.*,
    (now() - se.occurred_at) AS ago
FROM program.smoke_events se
WHERE se.occurred_at >= now() - interval '14 days';

;
