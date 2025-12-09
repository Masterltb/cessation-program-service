-- V42: Sync quiz_assignment_origin enum with Java Code
-- Adding missing values required for Weekly Quiz and Onboarding flow

-- 1. Add AUTO_WEEKLY (Required for Weekly Assessments)
ALTER TYPE program.quiz_assignment_origin ADD VALUE IF NOT EXISTS 'AUTO_WEEKLY';

-- 2. Add SYSTEM_ONBOARDING (Required for Baseline/Onboarding Quizzes)
ALTER TYPE program.quiz_assignment_origin ADD VALUE IF NOT EXISTS 'SYSTEM_ONBOARDING';
