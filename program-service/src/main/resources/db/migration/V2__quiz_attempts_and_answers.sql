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
