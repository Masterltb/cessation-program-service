-- R__ensure_program_schema.sql
-- Idempotent: an toàn chạy nhiều lần.

-----------------------------
-- SCHEMA & OWNERSHIP
-----------------------------
do $$
begin
  if not exists (select 1 from pg_namespace where nspname = 'program') then
    execute 'create schema program';
end if;

  -- Nếu user chạy script không có quyền đổi owner thì bỏ qua
begin
execute 'alter schema program owner to program_app_rw';
exception when insufficient_privilege then
    -- ignore nếu không đủ quyền
    null;
end;
end $$;

-----------------------------
-- ENUMS
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
    where n.nspname='program' and t.typname='quiz_scope'
  ) then
create type program.quiz_scope as enum ('system','coach');
end if;

  if not exists (
    select 1 from pg_type t join pg_namespace n on n.oid=t.typnamespace
    where n.nspname='program' and t.typname='severity_level'
  ) then
create type program.severity_level as enum ('LOW','MODERATE','HIGH','VERY_HIGH');
end if;
end $$;

-----------------------------
-- TABLE: programs (gốc để các FK trỏ vào)
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

-- Bổ sung cột còn thiếu
do $$
begin
  if not exists (
    select 1 from information_schema.columns
    where table_schema='program' and table_name='programs' and column_name='chatroom_id'
  ) then
alter table program.programs add column chatroom_id uuid;
end if;

  -- Nếu có bảng chatrooms thì tạo FK (tùy hệ thống của bạn)
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
-- TABLE: quiz_templates & phụ trợ
-----------------------------
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

-----------------------------
-- TABLE: quiz_assignments
-----------------------------
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

-----------------------------
-- TABLE: quiz_attempts
-----------------------------
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

-----------------------------
-- TABLE: quiz_answers (chuẩn: PK (attempt_id, question_no))
-- Nếu đã tồn tại, thêm cột/PK/FK còn thiếu
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
                                     answer      text,
                                     created_at  timestamptz not null default now(),
                                     primary key (attempt_id, question_no),
                                     constraint fk_qans_attempt foreign key (attempt_id)
                                         references program.quiz_attempts(id) on delete cascade
);
else
    -- đảm bảo attempt_id
    if not exists (
      select 1 from information_schema.columns
      where table_schema='program' and table_name='quiz_answers' and column_name='attempt_id'
    ) then
alter table program.quiz_answers add column attempt_id uuid;
-- TODO: backfill attempt_id từ dữ liệu thực tế của bạn trước khi set NOT NULL/PK
end if;

    -- đảm bảo PK (attempt_id, question_no)
    if not exists (
      select 1 from information_schema.table_constraints
      where table_schema='program' and table_name='quiz_answers'
        and constraint_type='PRIMARY KEY'
    ) then
begin
alter table program.quiz_answers add primary key (attempt_id, question_no);
exception when duplicate_table then null;
end;
end if;

    -- đảm bảo FK tới quiz_attempts
begin
alter table program.quiz_answers
    add constraint fk_qans_attempt foreign key (attempt_id)
        references program.quiz_attempts(id) on delete cascade;
exception when duplicate_object then null;
end;
end if;
end $$;

-----------------------------
-- TABLE: quiz_results
-----------------------------
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

-----------------------------
-- GRANTS (nếu có role program_app_rw)
-----------------------------
do $$
begin
  -- Có thể bỏ qua nếu không đủ quyền
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
