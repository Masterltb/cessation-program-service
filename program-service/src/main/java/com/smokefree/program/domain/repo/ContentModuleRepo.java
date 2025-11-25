package com.smokefree.program.domain.repo;

import com.smokefree.program.domain.model.ContentModule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentModuleRepo extends JpaRepository<ContentModule, UUID> {

    // Lấy version mới nhất theo code + lang
    Optional<ContentModule> findTopByCodeAndLangOrderByVersionDesc(String code, String lang);

    // Lấy toàn bộ version của 1 code + lang
    List<ContentModule> findByCodeAndLangOrderByVersionDesc(String code, String lang);

    // Dùng để kiểm tra trùng unique
    boolean existsByCodeAndLangAndVersion(String code, String lang, Integer version);

    // Dùng auto-next-version
    Integer findMaxVersionByCodeAndLang(String code, String lang);

    // Search cơ bản có phân trang
    Page<ContentModule> findByCodeContainingIgnoreCaseAndLang(
            String code,
            String lang,
            Pageable pageable
    );
}

