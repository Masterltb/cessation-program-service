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
