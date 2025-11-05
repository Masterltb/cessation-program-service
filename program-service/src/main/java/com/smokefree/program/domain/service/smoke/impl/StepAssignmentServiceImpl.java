package com.smokefree.program.domain.service.smoke.impl;


import com.smokefree.program.domain.model.Program;
import com.smokefree.program.domain.model.StepAssignment;
import com.smokefree.program.domain.model.StepStatus;
import com.smokefree.program.domain.repo.ProgramRepository;
import com.smokefree.program.domain.repo.StepAssignmentRepository;

import com.smokefree.program.domain.service.smoke.StepAssignmentService;
import com.smokefree.program.web.dto.step.CreateStepAssignmentReq;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StepAssignmentServiceImpl implements StepAssignmentService {

    private final StepAssignmentRepository stepRepo;
    private final ProgramRepository programRepo;

    @Override
    @Transactional(readOnly = true)
    public List<StepAssignment> listByProgram(UUID programId) {
        return stepRepo.findByProgramIdOrderByStepNoAsc(programId);
    }

    @Override
    @Transactional(readOnly = true)
    public StepAssignment getOne(UUID programId, UUID id) {
        return stepRepo.findByIdAndProgramId(id, programId)
                .orElseThrow(() -> notFound("StepAssignment", id));
    }

    @Override
    public StepAssignment create(UUID programId, CreateStepAssignmentReq req) {
        // 1) Đảm bảo Program tồn tại (nếu không cần dùng Program, chỉ để verify)
        programRepo.findById(programId)
                .orElseThrow(() -> notFound("Program", programId));

        // 2) (Tuỳ chọn) Kiểm tra trùng stepNo trong cùng program
        // -> cần có method tương ứng trong repository
        if (stepRepo.existsByProgramIdAndStepNo(programId, req.stepNo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "stepNo đã tồn tại trong chương trình này");
        }

        // 3) Tạo entity: KHÔNG dùng setProgram(...), chỉ gán programId
        // plannedDay: nếu business = ngày N trong kế hoạch, có thể đặt = stepNo
        StepAssignment sa = StepAssignment.builder()
                .programId(programId)
                .stepNo(req.stepNo())
                .plannedDay(req.stepNo())        // hoặc tính theo rule khác của bạn
                .note(req.note())                // note là optional
                .scheduledAt(req.eventAt())      // nếu null, có thể để null hoặc tự tính sau
                .status(StepStatus.PENDING)      // có default rồi, nhưng set rõ ràng cũng ok
                .build();

        return stepRepo.save(sa);
    }



    @Override
    public StepAssignment updateStatus(UUID programId, UUID id, StepStatus status) {
        StepAssignment sa = getOne(programId, id);
        sa.setStatus(status);
        return stepRepo.save(sa);
    }

    @Override
    public void delete(UUID programId, UUID id) {
        // Đảm bảo chỉ xóa step thuộc đúng program
        StepAssignment exist = stepRepo.findByIdAndProgramId(id, programId)
                .orElseThrow(() -> notFound("StepAssignment", id));
        stepRepo.deleteByIdAndProgramId(exist.getId(), programId);
    }

    // Helper
    private static boolean has(String s) { return s != null && !s.isBlank(); }

    private static ResponseStatusException notFound(String what, UUID id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, what + " not found: " + id);
    }
}
