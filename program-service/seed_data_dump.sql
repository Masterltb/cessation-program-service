-- ====================================================================
-- Aggregated Seed Data Dump from Flyway Migrations (Complete & Verifiable)
--
-- This file contains the full, unabridged seed data logic extracted
-- from the project's Flyway migrations. It is generated for validation
-- and to provide a complete snapshot of the initial database state.
-- This version includes all relevant seed data up to V41.
-- ====================================================================

-- Part 1: Seeding Plan Templates and Plan Steps
-- Source: V29__seed_plan_steps_levels_1_2_3.sql
-- --------------------------------------------------------------------

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Helper function to add a step to plan_steps
CREATE OR REPLACE FUNCTION program.add_step(
  tid uuid, d int, hhmm text, t text, det text, mm int
) RETURNS void
LANGUAGE plpgsql AS $fn$
BEGIN
  INSERT INTO program.plan_steps(id, template_id, day_no, slot, title, details, max_minutes, created_at)
  VALUES (gen_random_uuid(), tid, d, (hhmm)::time, t, det, mm, now())
  ON CONFLICT DO NOTHING; -- Make it safe to re-run
END
$fn$;

DO $seed_plans_and_steps$
DECLARE
  l1 uuid := '11111111-1111-1111-1111-111111111111'; -- L1: Thức Tỉnh (30d)
  l2 uuid := '22222222-2222-2222-2222-222222222222'; -- L2: Thay Đổi (45d)
  l3 uuid := '33333333-3333-3333-3333-333333333333'; -- L3: Tự Do (60d)
BEGIN
  -- Insert the 3 main plan templates.
  INSERT INTO program.plan_templates(id, level, code, name, total_days, created_at)
  VALUES
    (l1, 1, 'L1_30D', 'THỨC TỈNH (30 ngày)', 30, now()),
    (l2, 2, 'L2_45D', 'THAY ĐỔI (45 ngày)', 45, now()),
    (l3, 3, 'L3_60D', 'TỰ DO (60 ngày)', 60, now())
  ON CONFLICT (id) DO NOTHING;

  -- Seeding all steps for Level 1, 2, 3
  perform program.add_step(l1,1,'07:30','Check-in sáng + chọn mục tiêu 7 ngày','Kéo thang thèm/stress; chọn giấc ngủ; gợi ý: Thở 3’, Uống nước, Đi bộ 3’',10);
  perform program.add_step(l1,1,'12:30','Học 4D + tạo Urge Log thử','Chọn nguyên nhân & mức độ; chạy Timer 3’ để thực hành 4D',7);
  perform program.add_step(l1,1,'16:30','Lợi ích sau 24h bỏ thuốc (mini-slide + quiz)','3 slide + 3 câu hỏi',5);
  perform program.add_step(l1,1,'21:00','Nhật ký 2 câu + chọn Mantra','Viết 1–2 câu; chọn câu động lực (có giọng đọc)',12);
  perform program.add_step(l1,2,'07:30','Checklist dọn môi trường + nhắn người thân','Bỏ bật lửa/tàn thuốc; giặt áo ám mùi; nhắn người thân',12);
  perform program.add_step(l1,2,'12:30','Urge Log + Timer 3’ (4D)','Chọn nguyên nhân/mức độ; vượt cơn 3’',8);
  perform program.add_step(l1,2,'16:30','CBT nhanh: thay câu nghĩ “một điếu không sao”','Điền A–B–C và câu thay thế thực tế hơn',12);
  perform program.add_step(l1,2,'21:00','Tổng kết + Mantra','Viết ngắn; đặt câu cho ngày mai',12);
  perform program.add_step(l1,3,'07:30','Check-in + thở hướng dẫn 5’','Nếu thèm ≥7, nghe thở 5’',12);
  perform program.add_step(l1,3,'12:30','Kịch bản xã hội','Cà phê/đồng nghiệp/một mình – chọn câu từ chối',10);
  perform program.add_step(l1,3,'16:30','Mindfulness body-scan 5’','Nghe hướng dẫn, ghi 1 câu cảm nhận',12);
  perform program.add_step(l1,3,'21:00','Tổng kết + Mantra',null,12);
  perform program.add_step(l1,4,'07:30','Heatmap giờ hay thèm + áp lịch nhắc','Chọn 2–3 khung giờ nhắc',10);
  perform program.add_step(l1,4,'12:30','Chọn vật thay thế tay–miệng 24h','Kẹo the/tăm/đồ bóp tay',8);
  perform program.add_step(l1,4,'16:30','1 phiên Pomodoro 25–5 không thuốc',null,12);
  perform program.add_step(l1,4,'21:00','Tổng kết + Mantra',null,12);
  perform program.add_step(l1,5,'07:30','Tuyên ngôn “mình là người không hút” + 1 hành động','Ví dụ: 2 chai nước / đi bộ 10’',10);
  perform program.add_step(l1,5,'12:30','Urge Log + mini-quiz tim mạch','2 câu',10);
  perform program.add_step(l1,5,'16:30','Tập nói “không” (nghe mẫu + chọn câu)',null,12);
  perform program.add_step(l1,5,'21:00','Tổng kết + Mantra',null,12);
  perform program.add_step(l1,6,'07:30','Slip? Flow phục hồi','Chọn lý do và 1 hành động sửa; không reset toàn bộ streak',12);
  perform program.add_step(l1,6,'12:30','Kế hoạch cuối tuần không khói',null,10);
  perform program.add_step(l1,6,'16:30','Mindfulness 5’ + động viên',null,12);
  perform program.add_step(l1,6,'21:00','Tổng kết + Mantra',null,12);
  perform program.add_step(l1,7,'07:30','Khảo sát 7 ngày + Check-in',null,12);
  perform program.add_step(l1,7,'12:30','Rút kinh nghiệm: Top-3 chiến lược',null,10);
  perform program.add_step(l1,7,'16:30','Chọn phần thưởng nhỏ',null,10);
  perform program.add_step(l1,7,'21:00','Kế hoạch tuần sau','Chọn trọng tâm (ngủ/xã hội/Pomodoro...)',12);
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
  for i in 15..21 loop
    perform program.add_step(l1,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l1,i,'12:30','Tình huống rủi ro cao','Chọn kịch bản khó + phương án ứng phó',12);
    perform program.add_step(l1,i,'16:30','Theo dõi thói quen thay thế','Đánh dấu uống nước/đi bộ/giãn cơ',12);
    perform program.add_step(l1,i,'21:00','Journal + Mantra',null,12);
  end loop;
  for i in 22..29 loop
    perform program.add_step(l1,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l1,i,'12:30','Cập nhật tiền tiết kiệm','Nhập số tiền không chi cho thuốc hôm nay',8);
    perform program.add_step(l1,i,'16:30','Câu chuyện thành công + mini-quiz','Đọc 1 câu chuyện; trả lời 2 câu',10);
    perform program.add_step(l1,i,'21:00','Journal + Mantra',null,12);
  end loop;
  perform program.add_step(l1,30,'07:30','Tổng kết tiến trình 30 ngày','Xem huy hiệu/điểm/tiền tiết kiệm/heatmap',12);
  perform program.add_step(l1,30,'12:30','Cập nhật kế hoạch phòng tái','3 bẫy lớn + 3 hành động sẵn',12);
  perform program.add_step(l1,30,'21:00','Chọn lộ trình tiếp theo','Duy trì hoặc chuyển Level 2',12);
  perform program.add_step(l2,1,'07:30','Check-in + tư vấn NRT phối hợp','Đánh dấu dùng miếng dán/gum (tham khảo bác sĩ)',12);
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
  for i in 8..21 loop
    perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l2,i,'12:30','Urge log','Timer 3’ nếu cần',8);
    perform program.add_step(l2,i,'16:30','Kỹ năng phù hợp','Ngủ/giảm stress/exercise/CBT',12);
    perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
  end loop;
  for i in 22..28 loop
    perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l2,i,'12:30','Luân phiên thói quen','Pomodoro/mindfulness/exercise/xã hội',12);
    perform program.add_step(l2,i,'16:30','Cập nhật tiền tiết kiệm',null,8);
    perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
  end loop;
  update program.plan_steps set title='Tổng kết tuần + phần thưởng' where template_id=l2 and day_no=28 and slot='21:00';
  for i in 29..35 loop
    perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l2,i,'12:30','Luân phiên thói quen',null,12);
    perform program.add_step(l2,i,'16:30','Cập nhật tiền tiết kiệm',null,8);
    perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
  end loop;
  update program.plan_steps set title='Tổng kết tuần + phần thưởng' where template_id=l2 and day_no=35 and slot='21:00';
  for i in 36..42 loop
    perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l2,i,'12:30','Luân phiên thói quen',null,12);
    perform program.add_step(l2,i,'16:30','Cập nhật tiền tiết kiệm',null,8);
    perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
  end loop;
  update program.plan_steps set title='Tổng kết tuần + phần thưởng' where template_id=l2 and day_no=42 and slot='21:00';
  for i in 43..45 loop
    perform program.add_step(l2,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l2,i,'12:30','Luân phiên thói quen',null,12);
    perform program.add_step(l2,i,'16:30','Cập nhật tiền tiết kiệm',null,8);
    perform program.add_step(l2,i,'21:00','Nhật ký + Mantra',null,12);
  end loop;
  update program.plan_steps set title='Tổng kết tuần + phần thưởng' where template_id=l2 and day_no=45 and slot='21:00';
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
  for i in 4..14 loop
    perform program.add_step(l3,i,'07:30','Check-in sáng (nhắc dày tuần đầu)',null,12);
    perform program.add_step(l3,i,'12:30','Urge Log + 4D','Timer 3’',10);
    perform program.add_step(l3,i,'16:30','Luân phiên bài phù hợp','Thở/mindfulness/pomodoro/xã hội',12);
    perform program.add_step(l3,i,'21:00','Nhật ký + safety (nếu dùng thuốc)',null,12);
  end loop;
  for i in 15..28 loop
    perform program.add_step(l3,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l3,i,'12:30','Tình huống rủi ro cao','Chuẩn bị câu nói/động tác',12);
    perform program.add_step(l3,i,'16:30','Vận động nhẹ 10’','Đi bộ/giãn cơ',12);
    perform program.add_step(l3,i,'21:00','Nhật ký + Mantra',null,12);
  end loop;
  for i in 29..42 loop
    perform program.add_step(l3,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l3,i,'12:30','Kỹ năng xã hội nâng cao','Nói “không” khi đi nhậu/cà phê',12);
    perform program.add_step(l3,i,'16:30','Pomodoro 25–5',null,12);
    perform program.add_step(l3,i,'21:00','Nhật ký + Mantra',null,12);
  end loop;
  for i in 43..60 loop
    perform program.add_step(l3,i,'07:30','Check-in sáng',null,10);
    perform program.add_step(l3,i,'12:30','Kế hoạch phòng ngừa tái sử dụng','Chọn 1 bẫy + 1 hành động sẵn sàng',12);
    perform program.add_step(l3,i,'16:30','Củng cố bản sắc + mục tiêu thể lực','Đặt mục tiêu bước chân/đi bộ',12);
    perform program.add_step(l3,i,'21:00','Tổng kết tuần/huân chương','Nếu trùng mốc tuần thì trao huy hiệu',12);
  end loop;

END
$seed_plans_and_steps$;

-- Cleanup the helper function
DROP FUNCTION IF EXISTS program.add_step(uuid,int,text,text,text,int);


-- Part 2: Seeding Content Modules and Linking to Plan Steps
-- Source: V21__seed_content_modules_and_link_plan_steps.sql
-- --------------------------------------------------------------------

-- Helper function to upsert a content module
CREATE OR REPLACE FUNCTION program.put_module(
  p_code text, p_type text, p_lang text, p_version int, p_payload jsonb
) RETURNS uuid
LANGUAGE plpgsql AS $$
DECLARE rid uuid;
BEGIN
  SELECT id INTO rid FROM program.content_modules WHERE code=p_code AND lang=p_lang AND version=p_version;
  IF rid IS NOT NULL THEN RETURN rid; END IF;
  INSERT INTO program.content_modules(id, code, type, lang, version, payload, updated_at, created_at)
  VALUES (gen_random_uuid(), p_code, p_type, p_lang, p_version, p_payload, now(), now())
  RETURNING id INTO rid;
  RETURN rid;
END $$;

-- Seed the actual content modules
SELECT program.put_module('TASK_URGELOG_4D','TASK','vi',1,'{"title":"Urge Log + 4D", "howto":["Nhận diện cơn thèm (0-10)","Delay 3 phút","Deep breathing","Drink/Do: uống nước/đi bộ ngắn"], "timerSeconds":180, "fields":["trigger","intensity","note"]}'::jsonb);
SELECT program.put_module('EDU_BENEFITS_24H','EDU_SLIDES','vi',1,'{"title":"Lợi ích sau 24 giờ bỏ thuốc", "slides":[{"h":"Tim mạch","bullets":["Huyết áp ổn định hơn","Nhịp tim dần bình thường"]}, {"h":"Hô hấp","bullets":["CO giảm về mức bình thường","Oxy trong máu tăng lên"]}], "quiz":[{"q":"Sau 24h, CO thay đổi thế nào?","options":["Tăng","Giảm về bình thường"],"answer":1}]}'::jsonb);
SELECT program.put_module('EDU_BENEFITS_24H','EDU_SLIDES','vi',2,'{"title":"Lợi ích sau 24 giờ (v2)", "slides":[{"h":"Tim mạch","bullets":["Huyết áp giảm","Giảm co thắt mạch"]}, {"h":"Hô hấp","bullets":["CO giảm rõ","Oxy tăng"]}], "cta":{"text":"Tiếp tục lộ trình","deeplink":"app://plan/next"}}'::jsonb);
SELECT program.put_module('MINDSL_BODYSCAN_5M','AUDIO','vi',1,'{"title":"Mindfulness body-scan 5 phút", "audioUrl":"https://cdn.example.com/audio/bodyscan-5m.mp3", "transcript":"Hướng dẫn body-scan từ đầu đến chân", "durationSec":300}'::jsonb);
SELECT program.put_module('EDU_SOCIAL_SCRIPTS','EDU_TEMPLATES','vi',1,'{"title":"Kịch bản xã hội", "scenes":[{"name":"Cà phê","lines":["Cảm ơn, mình đang cai thuốc","Cho mình cốc nước nhé"]}, {"name":"Đồng nghiệp","lines":["Mình nghỉ thuốc rồi","Ra ngoài hít thở chút thay vì hút"]}]}'::jsonb);
SELECT program.put_module('TASK_POMODORO_25_5','TASK','vi',1,'{"title":"Pomodoro 25–5", "workMin":25,"breakMin":5, "tips":["Rời chỗ ngồi khi giải lao","Không kèm điếu thuốc"], "timer":true}'::jsonb);
SELECT program.put_module('PACK_STRESS_10M','PACK','vi',1,'{"title":"Bộ giảm stress 10 phút", "items":[{"type":"breath","label":"Thở 4-7-8 (2p)"}, {"type":"stretch","label":"Giãn cơ cổ vai (3p)"}, {"type":"walk","label":"Đi bộ chậm (5p)"}]}'::jsonb);
SELECT program.put_module('EDU_SLEEP_5HABITS','EDU_LIST','vi',1,'{"title":"5 thói quen ngủ", "items":["Giảm màn hình 1 giờ trước khi ngủ", "Tránh cà phê sau 15h", "Phòng mát/thoáng", "Giữ giờ ngủ cố định", "Thư giãn ngắn trước khi ngủ"]}'::jsonb);
SELECT program.put_module('TASK_WALK_10M','TASK','vi',1,'{"title":"Vận động nhẹ 10 phút", "actions":["Đi bộ 1000 bước hoặc giãn cơ toàn thân"], "timerSeconds":600}'::jsonb);
SELECT program.put_module('SOCIAL_SAY_NO','EDU_TEMPLATES','vi',1,'{"title":"Tập nói \"không\"", "phrases":["Mình nghỉ thuốc rồi, cảm ơn","Mình ra hít thở chút nhé"]}'::jsonb);
SELECT program.put_module('HIGH_RISK_SCENARIOS','EDU_PLANNER','vi',1,'{"title":"Tình huống rủi ro cao", "prompts":["Sau bữa ăn","Căng thẳng công việc","Khi buồn chán"], "planField":"Phương án ứng phó"}'::jsonb);
SELECT program.put_module('STORY_SUCCESS_MINIQUIZ','EDU_STORY','vi',1,'{"title":"Câu chuyện thành công", "story":"Sau 3 tuần, A hết thèm khi uống nước và đi bộ ngắn.", "quiz":[{"q":"Chiến lược hiệu quả nhất của A?","options":["Ngủ nhiều hơn","Uống nước + đi bộ"],"answer":1}]}'::jsonb);
SELECT program.put_module('RELAPSE_PREVENTION_PLAN','EDU_PLANNER','vi',1,'{"title":"Kế hoạch phòng tái sử dụng", "traps":["Tiệc với bạn","Căng thẳng","Một điếu không sao"], "actions":["Mang nước","Gọi buddy","Thở 3 phút + rời chỗ"]}'::jsonb);
SELECT program.put_module('EDU_BENEFITS_24H','EDU_SLIDES','en',1,'{"title":"24h Benefits","slides":[{"h":"Cardio","bullets":["BP stabilizes"]}]}'::jsonb);

-- Link modules to the plan steps and add one missing step
DO $$
DECLARE l1 uuid := '11111111-1111-1111-1111-111111111111';
    i int;
BEGIN
  UPDATE program.plan_steps SET module_code='TASK_URGELOG_4D' WHERE template_id=l1 AND day_no=1 AND slot='12:30'::time AND (module_code IS NULL OR module_code='');
  UPDATE program.plan_steps SET module_code='EDU_BENEFITS_24H' WHERE template_id=l1 AND day_no=1 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');
  UPDATE program.plan_steps SET module_code='EDU_SOCIAL_SCRIPTS' WHERE template_id=l1 AND day_no=3 AND slot='12:30'::time AND (module_code IS NULL OR module_code='');
  UPDATE program.plan_steps SET module_code='MINDSL_BODYSCAN_5M' WHERE template_id=l1 AND day_no=3 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');
  UPDATE program.plan_steps SET module_code='TASK_POMODORO_25_5' WHERE template_id=l1 AND day_no=4 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');
  UPDATE program.plan_steps SET module_code='PACK_STRESS_10M' WHERE template_id=l1 AND day_no=8 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');
  UPDATE program.plan_steps SET module_code='EDU_SLEEP_5HABITS' WHERE template_id=l1 AND day_no=9 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');
  UPDATE program.plan_steps SET module_code='TASK_WALK_10M' WHERE template_id=l1 AND day_no=10 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');
  UPDATE program.plan_steps SET module_code='EDU_SOCIAL_SCRIPTS' WHERE template_id=l1 AND day_no=11 AND slot='12:30'::time AND (module_code IS NULL OR module_code='');
  UPDATE program.plan_steps SET module_code='TASK_POMODORO_25_5' WHERE template_id=l1 AND day_no=12 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');
  UPDATE program.plan_steps SET module_code='SOCIAL_SAY_NO' WHERE template_id=l1 AND day_no=13 AND slot='16:30'::time AND (module_code IS NULL OR module_code='');
  FOR i IN 15..21 LOOP
    UPDATE program.plan_steps SET module_code='HIGH_RISK_SCENARIOS' WHERE template_id=l1 AND day_no=i AND slot='12:30'::time AND (module_code IS NULL OR module_code='');
  END LOOP;
  FOR i IN 22..29 LOOP
    UPDATE program.plan_steps SET module_code='STORY_SUCCESS_MINIQUIZ' WHERE template_id=l1 AND day_no=i AND slot='16:30'::time AND (module_code IS NULL OR module_code='');
  END LOOP;
  UPDATE program.plan_steps SET module_code='RELAPSE_PREVENTION_PLAN' WHERE template_id=l1 AND day_no=30 AND slot='12:30'::time AND (module_code IS NULL OR module_code='');

  INSERT INTO program.plan_steps(id, template_id, day_no, slot, title, details, max_minutes, created_at, module_code)
  SELECT gen_random_uuid(), l1, 30, '16:30'::time, 'Lễ huy hiệu + chia sẻ thành tựu', 'Nhìn lại 3 bài học rút ra', 10, now(), 'STORY_SUCCESS_MINIQUIZ'
  WHERE NOT EXISTS (
    SELECT 1 FROM program.plan_steps WHERE template_id=l1 AND day_no=30 AND slot='16:30'::time
  );
END $$;

DROP FUNCTION IF EXISTS program.put_module(text,text,text,int,jsonb);


-- Part 3: Seeding and Updating Streak Recovery Configurations
-- Source: V33 & V36
-- --------------------------------------------------------------------

-- First, clear any existing configurations to ensure a clean state, as done in V36.
DELETE FROM program.streak_recovery_configs;

-- Then, insert the final configurations from V36.
INSERT INTO program.streak_recovery_configs (attempt_order, module_code) VALUES
(1, 'RECOVERY_QUIZ_1'),
(2, 'RECOVERY_QUIZ_2'),
(3, 'RECOVERY_QUIZ_3');


-- Part 4: Seeding Badge Definitions
-- Source: V40__create_badge_system.sql
-- --------------------------------------------------------------------

INSERT INTO program.badges (id, code, category, level, name, description, icon_url, created_at) VALUES
-- Program Badges
('11111111-1111-1111-1111-111111111111', 'PROG_LV1', 'PROGRAM', 1, 'Khởi Hành', 'Bắt đầu hành trình cai thuốc lá.', 'assets/badges/prog_lv1.png', now()),
('11111111-1111-1111-1111-111111111112', 'PROG_LV2', 'PROGRAM', 2, 'Kiên Trì', 'Đi được một nửa chặng đường mà không tạm dừng.', 'assets/badges/prog_lv2.png', now()),
('11111111-1111-1111-1111-111111111113', 'PROG_LV3', 'PROGRAM', 3, 'Về Đích', 'Hoàn thành toàn bộ lộ trình cai thuốc.', 'assets/badges/prog_lv3.png', now()),
-- Streak Badges
('22222222-2222-2222-2222-222222222221', 'STREAK_LV1', 'STREAK', 1, 'Tuần Lễ Vàng', 'Đạt chuỗi 7 ngày không hút thuốc.', 'assets/badges/streak_lv1.png', now()),
('22222222-2222-2222-2222-222222222222', 'STREAK_LV2', 'STREAK', 2, 'Thói Quen Mới', 'Đạt chuỗi ngày bằng một nửa lộ trình.', 'assets/badges/streak_lv2.png', now()),
('22222222-2222-2222-2222-222222222223', 'STREAK_LV3', 'STREAK', 3, 'Chiến Binh Tự Do', 'Giữ vững chuỗi không hút thuốc suốt cả lộ trình.', 'assets/badges/streak_lv3.png', now()),
-- Quiz Badges
('33333333-3333-3333-3333-333333333331', 'QUIZ_LV1', 'QUIZ', 1, 'Tự Nhận Thức', 'Hoàn thành bài kiểm tra định kỳ đầu tiên.', 'assets/badges/quiz_lv1.png', now()),
('33333333-3333-3333-3333-333333333332', 'QUIZ_LV2', 'QUIZ', 2, 'Tiến Triển Tốt', 'Có kết quả kiểm tra cải thiện hoặc ổn định 2 lần liên tiếp.', 'assets/badges/quiz_lv2.png', now()),
('33333333-3333-3333-3333-333333333333', 'QUIZ_LV3', 'QUIZ', 3, 'Làm Chủ', 'Hoàn thành tất cả bài kiểm tra với mức độ phụ thuộc thấp.', 'assets/badges/quiz_lv3.png', now())
ON CONFLICT (id) DO NOTHING;


-- ====================================================================
-- End of Aggregated Seed Data Dump
-- ====================================================================

-- ====================================================================
-- Part 5: Seed Baseline Assessment Quiz (10 Questions)
-- This quiz is intended for new users to determine their dependency level.
-- ====================================================================
DO $$
DECLARE
    -- Declare a fixed UUID for the baseline quiz template for consistency
    quiz_template_uuid UUID := 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a10';
BEGIN

    -- Step 1: Insert the main Quiz Template record
    -- This quiz is identified by the unique code 'ONBOARDING_ASSESSMENT'
    INSERT INTO program.quiz_templates (id, code, name, version, status, scope, language_code, created_at, updated_at)
    VALUES (
        quiz_template_uuid,
        'ONBOARDING_ASSESSMENT',
        'Đánh giá Mức độ Lệ thuộc Nicotine',
        1,
        'PUBLISHED', -- Must be PUBLISHED to be usable by the system
        'SYSTEM',    -- This is a system-wide quiz
        'vi',
        now(),
        now()
    ) ON CONFLICT (id) DO NOTHING;

    -- Step 2: Insert all 10 questions linked to the template
    INSERT INTO program.quiz_template_questions (template_id, question_no, question_text, type)
    VALUES
        (quiz_template_uuid, 1, 'Bạn bắt đầu sử dụng/ hút thuốc lần đầu tiên sau khi thức dậy trong khoảng thời gian nào?', 'SINGLE_CHOICE'),
        (quiz_template_uuid, 2, 'Bạn có cảm thấy khó khăn khi kiềm chế việc sử dụng/ hút thuốc ở nơi bị cấm (như phòng họp, thư viện, nhà…)?', 'SINGLE_CHOICE'),
        (quiz_template_uuid, 3, 'Trong ngày sử dụng/ hút, lần đầu tiên trong buổi sáng có phải là lần bạn ‘ghét bỏ nhất’ nếu phải bỏ không?', 'SINGLE_CHOICE'),
        (quiz_template_uuid, 4, 'Trung bình mỗi ngày bạn sử dụng/ hút bao nhiêu lần?', 'SINGLE_CHOICE'),
        (quiz_template_uuid, 5, 'Bạn có dùng/ hút ngay cả khi bị ốm và phải nghỉ ngơi nhiều ngày?', 'SINGLE_CHOICE'),
        (quiz_template_uuid, 6, 'Bạn đã từng cố gắng giảm hoặc ngừng sử dụng/ hút mà thất bại?', 'SINGLE_CHOICE'),
        (quiz_template_uuid, 7, 'Việc sử dụng/ hút có gây ảnh hưởng đến gia đình, công việc, học hành hoặc mối quan hệ quan trọng nào của bạn?', 'SINGLE_CHOICE'),
        (quiz_template_uuid, 8, 'Khi bạn không sử dụng, bạn có cảm thấy bồn chồn, lo lắng hoặc ‘cồn cào’ muốn dùng ngay?', 'SINGLE_CHOICE'),
        (quiz_template_uuid, 9, 'Bạn có tăng liều hoặc tăng tần suất sử dụng/ hút để đạt được hiệu quả như trước không?', 'SINGLE_CHOICE'),
        (quiz_template_uuid, 10, 'Bạn có từng sử dụng/ hút lại sau một thời gian đã ngừng hoặc giảm đáng kể?', 'SINGLE_CHOICE')
    ON CONFLICT (template_id, question_no) DO NOTHING;

    -- Step 3: Insert all choices for each question with their respective scores (weight)
    -- Question 1
    INSERT INTO program.quiz_choice_labels (template_id, question_no, label_code, label_text, weight, is_correct)
    VALUES
        (quiz_template_uuid, 1, '1', '> 60 phút', 1, false),
        (quiz_template_uuid, 1, '2', '31-60 phút', 2, false),
        (quiz_template_uuid, 1, '3', '16-30 phút', 3, false),
        (quiz_template_uuid, 1, '4', '6-15 phút', 4, false),
        (quiz_template_uuid, 1, '5', '≤ 5 phút', 5, false)
    ON CONFLICT (template_id, question_no, label_code) DO NOTHING;

    -- Question 2
    INSERT INTO program.quiz_choice_labels (template_id, question_no, label_code, label_text, weight, is_correct)
    VALUES
        (quiz_template_uuid, 2, '1', 'Không bao giờ', 1, false),
        (quiz_template_uuid, 2, '2', 'Rất hiếm khi', 2, false),
        (quiz_template_uuid, 2, '3', 'Đôi khi', 3, false),
        (quiz_template_uuid, 2, '4', 'Thường xuyên', 4, false),
        (quiz_template_uuid, 2, '5', 'Luôn luôn', 5, false)
    ON CONFLICT (template_id, question_no, label_code) DO NOTHING;

    -- Question 3
    INSERT INTO program.quiz_choice_labels (template_id, question_no, label_code, label_text, weight, is_correct)
    VALUES
        (quiz_template_uuid, 3, '1', 'Không', 1, false),
        (quiz_template_uuid, 3, '2', 'Hiếm khi', 2, false),
        (quiz_template_uuid, 3, '3', 'Đôi khi', 3, false),
        (quiz_template_uuid, 3, '4', 'Thường xuyên', 4, false),
        (quiz_template_uuid, 3, '5', 'Luôn luôn', 5, false)
    ON CONFLICT (template_id, question_no, label_code) DO NOTHING;

    -- Question 4
    INSERT INTO program.quiz_choice_labels (template_id, question_no, label_code, label_text, weight, is_correct)
    VALUES
        (quiz_template_uuid, 4, '1', '≤ 5 lần', 1, false),
        (quiz_template_uuid, 4, '2', '6-10 lần', 2, false),
        (quiz_template_uuid, 4, '3', '11-20 lần', 3, false),
        (quiz_template_uuid, 4, '4', '21-30 lần', 4, false),
        (quiz_template_uuid, 4, '5', '> 30 lần', 5, false)
    ON CONFLICT (template_id, question_no, label_code) DO NOTHING;

    -- Question 5
    INSERT INTO program.quiz_choice_labels (template_id, question_no, label_code, label_text, weight, is_correct)
    VALUES
        (quiz_template_uuid, 5, '1', 'Không bao giờ', 1, false),
        (quiz_template_uuid, 5, '2', 'Rất hiếm', 2, false),
        (quiz_template_uuid, 5, '3', 'Đôi khi', 3, false),
        (quiz_template_uuid, 5, '4', 'Thường xuyên', 4, false),
        (quiz_template_uuid, 5, '5', 'Luôn luôn', 5, false)
    ON CONFLICT (template_id, question_no, label_code) DO NOTHING;

    -- Question 6
    INSERT INTO program.quiz_choice_labels (template_id, question_no, label_code, label_text, weight, is_correct)
    VALUES
        (quiz_template_uuid, 6, '1', 'Chưa bao giờ', 1, false),
        (quiz_template_uuid, 6, '2', '1 lần', 2, false),
        (quiz_template_uuid, 6, '3', '2-3 lần', 3, false),
        (quiz_template_uuid, 6, '4', '4-6 lần', 4, false),
        (quiz_template_uuid, 6, '5', '> 6 lần', 5, false)
    ON CONFLICT (template_id, question_no, label_code) DO NOTHING;

    -- Question 7
    INSERT INTO program.quiz_choice_labels (template_id, question_no, label_code, label_text, weight, is_correct)
    VALUES
        (quiz_template_uuid, 7, '1', 'Không ảnh hưởng', 1, false),
        (quiz_template_uuid, 7, '2', 'Rất ít', 2, false),
        (quiz_template_uuid, 7, '3', 'Có đôi chút', 3, false),
        (quiz_template_uuid, 7, '4', 'Ảnh hưởng đáng kể', 4, false),
        (quiz_template_uuid, 7, '5', 'Ảnh hưởng nghiêm trọng', 5, false)
    ON CONFLICT (template_id, question_no, label_code) DO NOTHING;

    -- Question 8
    INSERT INTO program.quiz_choice_labels (template_id, question_no, label_code, label_text, weight, is_correct)
    VALUES
        (quiz_template_uuid, 8, '1', 'Không bao giờ', 1, false),
        (quiz_template_uuid, 8, '2', 'Rất hiếm', 2, false),
        (quiz_template_uuid, 8, '3', 'Đôi khi', 3, false),
        (quiz_template_uuid, 8, '4', 'Thường xuyên', 4, false),
        (quiz_template_uuid, 8, '5', 'Hầu như luôn', 5, false)
    ON CONFLICT (template_id, question_no, label_code) DO NOTHING;

    -- Question 9
    INSERT INTO program.quiz_choice_labels (template_id, question_no, label_code, label_text, weight, is_correct)
    VALUES
        (quiz_template_uuid, 9, '1', 'Không bao giờ', 1, false),
        (quiz_template_uuid, 9, '2', 'Rất hiếm', 2, false),
        (quiz_template_uuid, 9, '3', 'Đôi khi', 3, false),
        (quiz_template_uuid, 9, '4', 'Thường xuyên', 4, false),
        (quiz_template_uuid, 9, '5', 'Luôn luôn', 5, false)
    ON CONFLICT (template_id, question_no, label_code) DO NOTHING;

    -- Question 10
    INSERT INTO program.quiz_choice_labels (template_id, question_no, label_code, label_text, weight, is_correct)
    VALUES
        (quiz_template_uuid, 10, '1', 'Chưa bao giờ', 1, false),
        (quiz_template_uuid, 10, '2', '1 lần', 2, false),
        (quiz_template_uuid, 10, '3', '2-3 lần', 3, false),
        (quiz_template_uuid, 10, '4', '4-6 lần', 4, false),
        (quiz_template_uuid, 10, '5', '> 6 lần', 5, false)
    ON CONFLICT (template_id, question_no, label_code) DO NOTHING;

END $$;

-- ====================================================================
-- Part 6: Missing Recovery Quizzes (CRITICAL FIX)
-- These templates are required by the Streak Recovery Configs defined in Part 3.
-- Code: RECOVERY_QUIZ_1, RECOVERY_QUIZ_2, RECOVERY_QUIZ_3
-- ====================================================================
DO $$
DECLARE
    rec_tpl_1 UUID := gen_random_uuid();
    rec_tpl_2 UUID := gen_random_uuid();
    rec_tpl_3 UUID := gen_random_uuid();
BEGIN
    -- 1. Recovery Quiz 1 (Nhận diện nguyên nhân)
    INSERT INTO program.quiz_templates (id, code, name, version, status, scope, language_code, created_at, updated_at)
    VALUES (rec_tpl_1, 'RECOVERY_QUIZ_1', 'Phục hồi: Nhận diện nguyên nhân', 1, 'PUBLISHED', 'SYSTEM', 'vi', now(), now())
    ON CONFLICT (id) DO UPDATE SET code = EXCLUDED.code, name = EXCLUDED.name, version = EXCLUDED.version, status = EXCLUDED.status, scope = EXCLUDED.scope, language_code = EXCLUDED.language_code, published_at = EXCLUDED.published_at, updated_at = EXCLUDED.updated_at;

    INSERT INTO program.quiz_template_questions (template_id, question_no, question_text, type)
    VALUES (rec_tpl_1, 1, 'Nguyên nhân chính khiến bạn hút lại điếu vừa rồi là gì?', 'SINGLE_CHOICE')
    ON CONFLICT (template_id, question_no) DO UPDATE SET question_text = EXCLUDED.question_text, type = EXCLUDED.type;

    INSERT INTO program.quiz_choice_labels (template_id, question_no, label_code, label_text, weight, is_correct)
    VALUES
        (rec_tpl_1, 1, 'A', 'Căng thẳng/Stress', 0, true),
        (rec_tpl_1, 1, 'B', 'Vui vẻ/Tiệc tùng', 0, true),
        (rec_tpl_1, 1, 'C', 'Buồn chán', 0, true),
        (rec_tpl_1, 1, 'D', 'Thói quen vô thức', 0, true)
    ON CONFLICT (template_id, question_no, label_code) DO UPDATE SET label_text = EXCLUDED.label_text, weight = EXCLUDED.weight, is_correct = EXCLUDED.is_correct;

    -- 2. Recovery Quiz 2 (Cam kết lại)
    INSERT INTO program.quiz_templates (id, code, name, version, status, scope, language_code, created_at, updated_at)
    VALUES (rec_tpl_2, 'RECOVERY_QUIZ_2', 'Phục hồi: Củng cố cam kết', 1, 'PUBLISHED', 'SYSTEM', 'vi', now(), now())
    ON CONFLICT (id) DO UPDATE SET code = EXCLUDED.code, name = EXCLUDED.name, version = EXCLUDED.version, status = EXCLUDED.status, scope = EXCLUDED.scope, language_code = EXCLUDED.language_code, published_at = EXCLUDED.published_at, updated_at = EXCLUDED.updated_at;

    INSERT INTO program.quiz_template_questions (template_id, question_no, question_text, type)
    VALUES (rec_tpl_2, 1, 'Bạn sẽ làm gì khác đi nếu gặp lại tình huống đó?', 'SINGLE_CHOICE')
    ON CONFLICT (template_id, question_no) DO UPDATE SET question_text = EXCLUDED.question_text, type = EXCLUDED.type;

    INSERT INTO program.quiz_choice_labels (template_id, question_no, label_code, label_text, weight, is_correct)
    VALUES
        (rec_tpl_2, 1, 'A', 'Tránh xa tình huống đó', 0, true),
        (rec_tpl_2, 1, 'B', 'Mang theo kẹo/nước thay thế', 0, true),
        (rec_tpl_2, 1, 'C', 'Gọi người hỗ trợ', 0, true)
    ON CONFLICT (template_id, question_no, label_code) DO UPDATE SET label_text = EXCLUDED.label_text, weight = EXCLUDED.weight, is_correct = EXCLUDED.is_correct;

    -- 3. Recovery Quiz 3 (Kế hoạch hành động)
    INSERT INTO program.quiz_templates (id, code, name, version, status, scope, language_code, created_at, updated_at)
    VALUES (rec_tpl_3, 'RECOVERY_QUIZ_3', 'Phục hồi: Kế hoạch hành động', 1, 'PUBLISHED', 'SYSTEM', 'vi', now(), now())
    ON CONFLICT (id) DO UPDATE SET code = EXCLUDED.code, name = EXCLUDED.name, version = EXCLUDED.version, status = EXCLUDED.status, scope = EXCLUDED.scope, language_code = EXCLUDED.language_code, published_at = EXCLUDED.published_at, updated_at = EXCLUDED.updated_at;

    INSERT INTO program.quiz_template_questions (template_id, question_no, question_text, type)
    VALUES (rec_tpl_3, 1, 'Mức độ tự tin của bạn để quay lại chuỗi ngay bây giờ?', 'SINGLE_CHOICE')
    ON CONFLICT (template_id, question_no) DO UPDATE SET question_text = EXCLUDED.question_text, type = EXCLUDED.type;

    INSERT INTO program.quiz_choice_labels (template_id, question_no, label_code, label_text, weight, is_correct)
    VALUES
        (rec_tpl_3, 1, 'A', 'Rất tự tin (100%)', 0, true),
        (rec_tpl_3, 1, 'B', 'Khá tự tin (70-90%)', 0, true),
        (rec_tpl_3, 1, 'C', 'Cần thêm hỗ trợ', 0, true)
    ON CONFLICT (template_id, question_no, label_code) DO UPDATE SET label_text = EXCLUDED.label_text, weight = EXCLUDED.weight, is_correct = EXCLUDED.is_correct;
END $$;
-- ====================================================================
-- Part 7: Seed Weekly Quiz Schedules (REQUIRED FOR WEEKLY CHECK-INS)
-- Logic: Assign 'WEEKLY_ASSESSMENT' starting day 7, repeating every 7 days.
-- ====================================================================
DO $$
DECLARE
    -- Lấy ID của các Plan Template đã seed ở Part 1
    l1_id UUID := '11111111-1111-1111-1111-111111111111'; -- L1 (30 ngày)
    l2_id UUID := '22222222-2222-2222-2222-222222222222'; -- L2 (45 ngày)
    l3_id UUID := '33333333-3333-3333-3333-333333333333'; -- L3 (60 ngày)

    -- Tạo hoặc lấy ID cho Quiz Template đánh giá tuần
    weekly_quiz_id UUID := gen_random_uuid();
BEGIN
    -- 1. Tạo Quiz Template cho bài đánh giá tuần (nếu chưa có)
    INSERT INTO program.quiz_templates (id, code, name, version, status, scope, language_code, created_at, updated_at)
    VALUES (weekly_quiz_id, 'WEEKLY_ASSESSMENT', 'Đánh giá tiến độ hàng tuần', 1, 'PUBLISHED', 'SYSTEM', 'vi', now(), now())
    ON CONFLICT (id) DO UPDATE SET code = EXCLUDED.code, name = EXCLUDED.name, version = EXCLUDED.version, status = EXCLUDED.status, scope = EXCLUDED.scope, language_code = EXCLUDED.language_code, published_at = EXCLUDED.published_at, updated_at = EXCLUDED.updated_at RETURNING id INTO weekly_quiz_id;

    -- THÊM CÁC CÂU HỎI VÀ LỰA CHỌN CHO WEEKLY_ASSESSMENT TẠI ĐÂY
    -- Ví dụ:
    INSERT INTO program.quiz_template_questions (template_id, question_no, question_text, type) VALUES
    (weekly_quiz_id, 1, 'Mức độ thèm thuốc trung bình của bạn trong tuần qua là bao nhiêu? (0-Không thèm, 10-Cực kỳ thèm)', 'SINGLE_CHOICE'),
    (weekly_quiz_id, 2, 'Bạn có gặp tình huống khó khăn nào trong việc tránh hút thuốc không?', 'SINGLE_CHOICE'), -- Changed from TEXT_INPUT
    (weekly_quiz_id, 3, 'Bạn cảm thấy tự tin bao nhiêu về khả năng duy trì chuỗi không hút thuốc trong tuần tới? (0-Không tự tin, 10-Rất tự tin)', 'SINGLE_CHOICE');

    INSERT INTO program.quiz_choice_labels (template_id, question_no, label_code, label_text, weight, is_correct) VALUES
    (weekly_quiz_id, 1, '1', '0-2', 0, false),
    (weekly_quiz_id, 1, '2', '3-5', 0, false),
    (weekly_quiz_id, 1, '3', '6-8', 0, false),
    (weekly_quiz_id, 1, '4', '9-10', 0, false),
    (weekly_quiz_id, 2, '1', 'Có', 0, false), -- Placeholder choice for original TEXT_INPUT question
    (weekly_quiz_id, 2, '2', 'Không', 0, false), -- Placeholder choice
    (weekly_quiz_id, 3, '1', '0-4', 0, false),
    (weekly_quiz_id, 3, '2', '5-7', 0, false),
    (weekly_quiz_id, 3, '3', '8-10', 0, false);


    -- 2. Cấu hình Lịch (Schedule) cho Level 1 (30 ngày)
    -- Bắt đầu ngày 7, lặp lại mỗi 7 ngày (7, 14, 21, 28...)
    INSERT INTO program.plan_quiz_schedules (id, plan_template_id, quiz_template_id, start_offset_day, every_days, order_no, created_at)
    VALUES (gen_random_uuid(), l1_id, weekly_quiz_id, 7, 7, 1, now())
    ON CONFLICT DO NOTHING;

    -- 3. Cấu hình Lịch cho Level 2 (45 ngày)
    INSERT INTO program.plan_quiz_schedules (id, plan_template_id, quiz_template_id, start_offset_day, every_days, order_no, created_at)
    VALUES (gen_random_uuid(), l2_id, weekly_quiz_id, 7, 7, 1, now())
    ON CONFLICT DO NOTHING;

    -- 4. Cấu hình Lịch cho Level 3 (60 ngày)
    INSERT INTO program.plan_quiz_schedules (id, plan_template_id, quiz_template_id, start_offset_day, every_days, order_no, created_at)
    VALUES (gen_random_uuid(), l3_id, weekly_quiz_id, 7, 7, 1, now())
    ON CONFLICT DO NOTHING;

END $$;