-- Step 1: attachable_type, attachable_id 컬럼 추가
-- (ddl-auto: update 환경에서는 이미 자동으로 추가되었을 수 있음)
ALTER TABLE file_meta_data ADD COLUMN IF NOT EXISTS attachable_type VARCHAR(20);
ALTER TABLE file_meta_data ADD COLUMN IF NOT EXISTS attachable_id BIGINT;

-- Step 2: 기존 post_id 데이터를 새 컬럼으로 마이그레이션
UPDATE file_meta_data
SET attachable_type = 'POST',
    attachable_id = post_id
WHERE post_id IS NOT NULL;

-- Step 3: post_id FK 제거 (마이그레이션 확인 후 실행)
-- ALTER TABLE file_meta_data DROP CONSTRAINT IF EXISTS fk_file_meta_data_post;
-- ALTER TABLE file_meta_data DROP COLUMN IF EXISTS post_id;
