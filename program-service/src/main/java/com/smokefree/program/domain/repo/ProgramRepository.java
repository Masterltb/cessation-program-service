package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.Program;
import com.smokefree.program.domain.model.ProgramStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramRepository extends JpaRepository<Program, UUID> {

    // dùng cho ProgramServiceImpl#createProgram và #getActive
    Optional<Program> findFirstByUserIdAndStatusAndDeletedAtIsNull(UUID userId, ProgramStatus status);

    // có thể giữ lại nếu cần cho nơi khác
    Optional<Program> findByUserId(UUID userId);

    // dùng khi coach cần xác thực quyền trên program
    boolean existsByIdAndCoachId(UUID id, UUID coachId);

    @Query("select p.id from Program p where p.id in :ids")
    List<UUID> findIdsByIdIn(Collection<UUID> ids);

    @Query("select p.id from Program p where p.id in :ids and p.coachId = :coachId")
    List<UUID> findIdsByIdInAndCoachId(Collection<UUID> ids, UUID coachId);

    boolean existsById(UUID id);                // vẫn giữ để xài chỗ khác

}
