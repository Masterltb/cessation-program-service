package com.smokefree.program.domain.service.smoke.impl;

import com.smokefree.program.domain.model.Program;
import com.smokefree.program.domain.model.SmokeEvent;
import com.smokefree.program.domain.model.SmokeEventKind;
import com.smokefree.program.domain.repo.ProgramRepository;
import com.smokefree.program.domain.repo.SmokeEventRepository;
import com.smokefree.program.domain.repo.StreakRecoveryConfigRepository;
import com.smokefree.program.domain.service.QuizAssignmentService;
import com.smokefree.program.domain.service.smoke.SmokeEventService;
// import com.smokefree.program.domain.service.smoke.StepAssignmentService; // Tạm thời không dùng
import com.smokefree.program.domain.service.smoke.StreakService;
import com.smokefree.program.web.dto.smoke.CreateSmokeEventReq;
import com.smokefree.program.web.dto.smoke.SmokeEventStatisticsRes;
import com.smokefree.program.web.error.ForbiddenException;
import com.smokefree.program.web.error.NotFoundException;
import com.smokefree.program.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation của SmokeEventService.
 * <p>
 * Class này xử lý các logic nghiệp vụ cốt lõi liên quan đến sự kiện hút thuốc:
 * <ul>
 *     <li>Ghi nhận sự kiện (Hút thuốc, Cơn thèm).</li>
 *     <li>Xử lý tác động của sự kiện lên chuỗi (Streak): Reset khi tái nghiện, đóng băng khi lỡ hút.</li>
 *     <li>Kích hoạt quy trình phục hồi (Recovery Flow) khi người dùng lỡ hút (Slip).</li>
 *     <li>Cung cấp thống kê và lịch sử sự kiện.</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmokeEventServiceImpl implements SmokeEventService {

    private final SmokeEventRepository smokeEventRepository;
    private final ProgramRepository programRepository;
    private final StreakService streakService;
    private final StreakRecoveryConfigRepository recoveryConfigRepo;
    private final QuizAssignmentService quizAssignmentService;
    // private final StepAssignmentService stepAssignmentService;

    /**
     * Tạo mới một sự kiện hút thuốc hoặc cơn thèm.
     * <p>
     * Đây là hàm quan trọng nhất, quyết định số phận của chuỗi (Streak) dựa trên loại sự kiện:
     * <ul>
     *     <li><b>RELAPSE (Tái nghiện):</b> Reset cứng chuỗi về 0.</li>
     *     <li><b>SLIP (Lỡ hút):</b> Đánh dấu gãy chuỗi hiện tại, nhưng cho phép cơ hội phục hồi (Recovery) nếu còn lượt.</li>
     *     <li><b>URGE/SMOKE (Khác):</b> Duy trì hoặc tăng chuỗi nếu người dùng vượt qua được.</li>
     * </ul>
     * </p>
     *
     * @param programId ID của chương trình.
     * @param req       DTO chứa thông tin sự kiện.
     * @return Entity SmokeEvent đã được lưu.
     */
    @Override
    @Transactional
    public SmokeEvent create(UUID programId, CreateSmokeEventReq req) {
        // 1. Kiểm tra quyền truy cập (Owner/Coach) và trạng thái gói (Trial expired?)
        ensureProgramAccess(programId, false);
        log.info("[SmokeEvent] create for programId: {}, kind: {}", programId, req.kind());

        // 2. Load Program để lấy thông tin streak hiện tại
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));
        log.debug("[SmokeEvent] Program loaded: id={}, currentStreak={}, bestStreak={}, recoveryUsedCount={}",
                program.getId(), program.getStreakCurrent(), program.getStreakBest(), program.getStreakRecoveryUsedCount());

        // 3. Tạo và lưu SmokeEvent vào DB
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        SmokeEvent event = SmokeEvent.builder()
                .programId(programId)
                .userId(program.getUserId())
                .kind(req.kind())
                .eventType(req.eventType())
                .occurredAt(req.occurredAt() != null ? req.occurredAt() : now)
                .eventAt(req.eventAt() != null ? req.eventAt() : now)
                .note(req.note())
                .puffs(req.puffs())
                .reason(req.reason())
                .build();
        log.debug("[SmokeEvent] New SmokeEvent built: kind={}", event.getKind());

        smokeEventRepository.save(event);
        log.debug("[SmokeEvent] SmokeEvent saved (first time): id={}", event.getId());

        // Cập nhật thời điểm hút thuốc cuối cùng
        program.setLastSmokeAt(now);
        log.debug("[SmokeEvent] Program.lastSmokeAt updated to {}", now);

        // 4. Xử lý Logic Streak dựa trên loại sự kiện (Kind)
        if (req.kind() == SmokeEventKind.RELAPSE) {
            // --- TRƯỜNG HỢP TÁI NGHIỆN (RELAPSE) ---
            log.warn("[SmokeEvent] RELAPSE detected for programId: {}. Performing HARD RESET.", programId);
            
            // A. Ngắt chuỗi hiện tại và ghi log
            streakService.breakStreakAndLog(programId, now, event.getId(), req.reason(), req.note()); // <-- SỬA Ở ĐÂY

            // B. KHÔNG kích hoạt phục hồi (Recovery) vì đây là tái nghiện hoàn toàn

            // C. Bắt đầu một chuỗi mới từ con số 0 ngay lập tức
            streakService.start(programId, now);

        } else if (req.kind() == SmokeEventKind.SLIP) {
            // --- TRƯỜNG HỢP LỠ HÚT (SLIP) ---
            log.info("[SmokeEvent] SLIP detected. Breaking streak and attempting recovery for programId: {}", programId);
            
            // A. Ngắt chuỗi hiện tại
            var breakRecord = streakService.breakStreakAndLog(programId, now, event.getId(), req.reason(), req.note()); // <-- SỬA Ở ĐÂY
            log.debug("[SmokeEvent] Streak break has been logged.");

            // B. Cố gắng gán bài tập phục hồi (Recovery Quiz)
            boolean recoveryAssigned = handleRecoveryAssignment(program, breakRecord.getId());

            // C. Nếu không còn bài phục hồi nào (hết lượt), coi như tái nghiện -> Reset cứng
            if (!recoveryAssigned) {
                log.warn("[SmokeEvent] No recovery was assigned. Performing hard reset by starting a new streak.");
                streakService.start(programId, now);
            }

        } else {
            // --- TRƯỜNG HỢP BÌNH THƯỜNG (URGE passed, hoặc logic khác) ---
            log.info("[SmokeEvent] Continuing/starting streak for programId: {}", programId);
            
            // Đảm bảo streak đang chạy
            streakService.startOrContinueStreak(programId);
            
            // Tăng biến đếm streak trong Program (Snapshot để hiển thị nhanh)
            int currentStreak = program.getStreakCurrent() + 1;
            program.setStreakCurrent(currentStreak);
            log.debug("[SmokeEvent] Program.streakCurrent incremented to {}", currentStreak);

            // Cập nhật kỷ lục nếu phá vỡ kỷ lục cũ
            if (currentStreak > program.getStreakBest()) {
                program.setStreakBest(currentStreak);
                log.debug("[SmokeEvent] Program.streakBest updated to {}", currentStreak);
            }
        }

        // 5. Lưu trạng thái mới của Program
        programRepository.save(program);
        log.debug("[SmokeEvent] Program saved: id={}, currentStreak={}, bestStreak={}, recoveryUsedCount={}",
                program.getId(), program.getStreakCurrent(), program.getStreakBest(), program.getStreakRecoveryUsedCount());

        return event;
    }

    /**
     * Xử lý việc gán bài tập phục hồi (Recovery Quiz) khi người dùng bị Slip.
     *
     * @param program       Chương trình hiện tại.
     * @param streakBreakId ID của bản ghi gãy chuỗi (StreakBreak), dùng để liên kết kết quả phục hồi sau này.
     * @return true nếu gán thành công, false nếu không tìm thấy cấu hình (hết lượt phục hồi).
     */
    private boolean handleRecoveryAssignment(Program program, UUID streakBreakId) {
        // Tính toán lượt phục hồi tiếp theo (Lần 1, Lần 2...)
        int nextAttemptOrder = program.getStreakRecoveryUsedCount() + 1;
        log.info("[Recovery] Attempting to find recovery config for attempt #{}", nextAttemptOrder);

        // Tìm cấu hình bài tập tương ứng với lượt này
        Optional<com.smokefree.program.domain.model.StreakRecoveryConfig> configOpt = recoveryConfigRepo.findByAttemptOrder(nextAttemptOrder);

        if (configOpt.isEmpty()) {
            log.warn("[Recovery] No recovery config found for attempt #{}. No recovery task will be assigned.", nextAttemptOrder);
            return false; // Hết bài phục hồi -> Trả về false để bên ngoài thực hiện Hard Reset
        }

        var config = configOpt.get();
        log.info("[Recovery] Found recovery config: attemptOrder={}, moduleCode={}", config.getAttemptOrder(), config.getModuleCode());

        String moduleCode = config.getModuleCode();

        // Gọi service để tạo QuizAssignment cho người dùng
        log.info("[Recovery] Assigning recovery quiz with module code: {}", moduleCode);
        quizAssignmentService.assignRecoveryQuiz(program.getId(), moduleCode, streakBreakId);

        return true;
    }

    /**
     * Lấy lịch sử các sự kiện hút thuốc của một chương trình.
     *
     * @param programId ID chương trình.
     * @param size      Số lượng bản ghi tối đa muốn lấy.
     * @return Danh sách sự kiện, sắp xếp mới nhất trước.
     */
    @Override
    @Transactional(readOnly = true)
    public List<SmokeEvent> getHistory(UUID programId, int size) {
        ensureProgramAccess(programId, true);
        return smokeEventRepository.findByProgramIdOrderByEventAtDesc(programId).stream()
            .limit(Math.max(1, size))
            .toList();
    }

    /**
     * Tính toán thống kê sự kiện theo khoảng thời gian (Tuần/Tháng).
     *
     * @param programId ID chương trình.
     * @param period    Khoảng thời gian ("WEEK", "MONTH" hoặc null cho toàn bộ).
     * @return DTO chứa tổng số lần, trung bình mỗi ngày, v.v.
     */
    @Override
    @Transactional(readOnly = true)
    public SmokeEventStatisticsRes getStatistics(UUID programId, String period) {
        ensureProgramAccess(programId, true);
        List<SmokeEvent> allEvents = smokeEventRepository.findByProgramIdOrderByEventAtDesc(programId);

        // Fix Timezone: Luôn sử dụng UTC để tính toán nhất quán
        LocalDate nowUtc = LocalDate.now(ZoneOffset.UTC);
        LocalDate cutoffDate = nowUtc;
        
        if ("WEEK".equals(period)) {
            cutoffDate = cutoffDate.minusWeeks(1);
        } else if ("MONTH".equals(period)) {
            cutoffDate = cutoffDate.minusMonths(1);
        }

        LocalDate finalCutoffDate = cutoffDate;
        List<SmokeEvent> filteredEvents = allEvents.stream()
            .filter(e -> e.getOccurredAt().atZoneSameInstant(ZoneOffset.UTC).toLocalDate().isAfter(finalCutoffDate))
            .toList();

        int totalCount = filteredEvents.size();
        // Tính trung bình mỗi ngày
        double avgPerDay = totalCount > 0 ? (double) totalCount / Math.max(1, ChronoUnit.DAYS.between(cutoffDate, nowUtc)) : 0.0;

        return new SmokeEventStatisticsRes(
            totalCount,
            filteredEvents.size(), 
            avgPerDay,
            List.of()
        );
    }

    /**
     * Helper method để kiểm tra quyền truy cập vào chương trình.
     * <p>
     * Các quy tắc:
     * <ul>
     *     <li>ADMIN luôn có quyền.</li>
     *     <li>Owner (chủ sở hữu) có quyền đọc/ghi.</li>
     *     <li>Coach (huấn luyện viên) có quyền đọc, và ghi nếu allowCoachWrite = true.</li>
     *     <li>Nếu chương trình dùng thử đã hết hạn, chặn truy cập (SubscriptionRequiredException).</li>
     * </ul>
     * </p>
     */
    private void ensureProgramAccess(UUID programId, boolean allowCoachWrite) {
        if (SecurityUtil.hasRole("ADMIN")) {
            return;
        }
        UUID userId = SecurityUtil.requireUserId();
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new NotFoundException("Program not found: " + programId));

        // Chặn truy cập nếu chương trình dùng thử đã quá hạn
        if (program.getTrialEndExpected() != null && java.time.Instant.now().isAfter(program.getTrialEndExpected())) {
            throw new com.smokefree.program.web.error.SubscriptionRequiredException("Trial expired");
        }

        boolean isOwner = program.getUserId().equals(userId);
        boolean isCoach = program.getCoachId() != null && program.getCoachId().equals(userId) && SecurityUtil.hasRole("COACH");
        if (!isOwner && !isCoach) {
            throw new ForbiddenException("Access denied for program " + programId);
        }
        if (isCoach && !allowCoachWrite) {
            throw new ForbiddenException("Coach cannot modify smoke events for program " + programId);
        }
    }
}
