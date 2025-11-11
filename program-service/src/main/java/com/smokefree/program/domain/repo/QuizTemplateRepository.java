package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.AssignmentScope;
import com.smokefree.program.domain.model.QuizTemplate;
import com.smokefree.program.domain.model.QuizTemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuizTemplateRepository extends JpaRepository<QuizTemplate, UUID> {

    // Coach templates: scope="coach" và ownerId = coachId
    List<QuizTemplate> findByScopeAndOwnerId(String scope, UUID ownerId);

    // System templates: scope="system" và ownerId IS NULL
    List<QuizTemplate> findByScopeAndOwnerIdIsNull(String scope);

    // Tra cứu duy nhất theo (name, scope, ownerId, version)
    Optional<QuizTemplate> findByNameAndScopeAndOwnerIdAndVersion(
            String name, String scope, UUID ownerId, Integer version
    );

    // Ví dụ tiện dụng (tuỳ bạn dùng hay không)
    List<QuizTemplate> findByScopeAndOwnerIdAndStatus(String scope, UUID ownerId, QuizTemplateStatus status);
    List<QuizTemplate> findByScopeAndOwnerIdIsNullAndStatus(String scope, QuizTemplateStatus status);

    Optional<QuizTemplate> findByNameAndScopeAndOwnerIdAndVersion(
            String name, AssignmentScope scope, UUID ownerId, Integer version);
}
