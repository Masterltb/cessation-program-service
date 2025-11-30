package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.StepAssignment;
import com.smokefree.program.domain.model.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    long countByProgramIdAndStatusNot(UUID programId, StepStatus status);
    long countByProgramIdAndStepNoLessThanAndStatusNot(
            UUID programId,
            Integer stepNo,
            StepStatus status
    );

    List<StepAssignment> findByProgramIdAndPlannedDay(UUID programId, int plannedDay);

    /**
     * Đếm số lượng step chưa hoàn thành (không phải COMPLETED) cho một ngày cụ thể của chương trình.
     */
    @Query("SELECT COUNT(s) FROM StepAssignment s WHERE s.programId = :programId AND s.plannedDay = :plannedDay AND s.status <> com.smokefree.program.domain.model.StepStatus.COMPLETED")
    long countIncompleteStepsForDay(@Param("programId") UUID programId, @Param("plannedDay") int plannedDay);
}
