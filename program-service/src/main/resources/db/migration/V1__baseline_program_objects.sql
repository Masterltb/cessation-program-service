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
