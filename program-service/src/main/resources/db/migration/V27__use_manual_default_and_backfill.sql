-- Đặt default và backfill sau khi V26 đã COMMIT
alter table program.quiz_assignments
    alter column origin set default 'MANUAL';

update program.quiz_assignments
set origin = 'MANUAL'
where origin is null;
