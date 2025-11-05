package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.StepAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface StepAssignmentRepository extends JpaRepository<StepAssignment, UUID> {

    // Sắp xếp theo stepNo thay cho orderIndex
    List<StepAssignment> findByProgramIdOrderByStepNoAsc(UUID programId);

    Optional<StepAssignment> findByIdAndProgramId(UUID id, UUID programId);

    // Lấy stepNo lớn nhất trong chương trình (nếu cần auto-number)
    Optional<StepAssignment> findTopByProgramIdOrderByStepNoDesc(UUID programId);

    // Kiểm tra trùng stepNo trong cùng program
    boolean existsByProgramIdAndStepNo(UUID programId, Integer stepNo);

    // (tuỳ chọn) nếu bạn cần truy vấn trực tiếp theo stepNo
    Optional<StepAssignment> findByProgramIdAndStepNo(UUID programId, Integer stepNo);

    void deleteByIdAndProgramId(UUID id, UUID programId);
}
