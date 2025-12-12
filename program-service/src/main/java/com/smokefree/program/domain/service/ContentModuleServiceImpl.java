package com.smokefree.program.domain.service;

import com.smokefree.program.domain.model.ContentModule;
import com.smokefree.program.domain.repo.ContentModuleRepository; // <-- SỬA Ở ĐÂY
import com.smokefree.program.web.dto.module.*;
import com.smokefree.program.web.error.NotFoundException;
import com.smokefree.program.web.error.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Triển khai của ContentModuleService.
 * Quản lý các module nội dung (bài học, mẹo, thông tin bổ trợ) với cơ chế phiên bản (versioning).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentModuleServiceImpl implements ContentModuleService {

    private final ContentModuleRepository contentModuleRepo; // <-- SỬA Ở ĐÂY

    /**
     * Tạo mới một module nội dung.
     * Nếu không chỉ định version, hệ thống tự động tăng version dựa trên bản mới nhất hiện có.
     *
     * @param req Yêu cầu tạo mới chứa thông tin module.
     * @return Thông tin module vừa tạo.
     */
    @Override
    @Transactional
    public ContentModuleRes create(ContentModuleCreateReq req) {
        String lang = normalizeLang(req.lang());
        String code = req.code().trim();
        String type = req.type().trim();

        Integer version = req.version();
        if (version == null) {
            // Tìm version lớn nhất hiện tại để +1
            var latestOpt = contentModuleRepo.findTopByCodeAndLangOrderByVersionDesc(code, lang);
            version = latestOpt.map(m -> m.getVersion() + 1).orElse(1);
        }

        // Kiểm tra trùng lặp
        boolean exists = contentModuleRepo.existsByCodeAndLangAndVersion(code, lang, version);
        if (exists) {
            throw new ValidationException("Module đã tồn tại với code=" + code
                    + ", lang=" + lang + ", version=" + version);
        }

        ContentModule entity = ContentModule.builder()
                .code(code)
                .type(type)
                .lang(lang)
                .version(version)
                .payload(req.payload())
                .updatedAt(OffsetDateTime.now())
                .build();

        ContentModule saved = contentModuleRepo.save(entity);
        return toRes(saved);
    }


    /**
     * Cập nhật module nội dung bằng cách tạo một phiên bản mới (Versioning Strategy).
     * Không ghi đè bản ghi cũ để giữ lịch sử.
     *
     * @param id  ID của module gốc (để lấy code và lang).
     * @param req Yêu cầu cập nhật chứa nội dung mới.
     * @return Thông tin phiên bản module mới vừa tạo.
     */
    @Override
    @Transactional
    public ContentModuleRes update(UUID id, ContentModuleUpdateReq req) {
        // 1. Tìm module gốc để lấy code và lang
        ContentModule baseModule = contentModuleRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("ContentModule not found: " + id));

        String code = baseModule.getCode();
        String lang = baseModule.getLang();

        // 2. Tìm phiên bản mới nhất và +1 để tạo version mới
        int newVersion = contentModuleRepo.findTopByCodeAndLangOrderByVersionDesc(code, lang)
                .map(latest -> latest.getVersion() + 1)
                .orElse(2); // Nếu không tìm thấy (chỉ có bản gốc), version mới sẽ là 2

        // 3. Tạo một entity *mới* để tạo ra phiên bản mới
        ContentModule newVersionModule = ContentModule.builder()
                .code(code)
                .lang(lang)
                .version(newVersion)
                .type(req.type().trim())
                .payload(req.payload())
                // id và updatedAt sẽ được tự động gán bởi @PrePersist
                .build();

        // 4. Lưu entity mới vào DB
        ContentModule savedModule = contentModuleRepo.save(newVersionModule);

        // 5. Trả về DTO của entity mới
        return toRes(savedModule);
    }

    /**
     * Xóa một phiên bản module cụ thể.
     *
     * @param id ID của module cần xóa.
     */
    @Override
    @Transactional
    public void delete(UUID id) {
        ContentModule entity = contentModuleRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("ContentModule not found: " + id));
        contentModuleRepo.delete(entity);
    }

    /**
     * Lấy chi tiết một module theo ID.
     *
     * @param id ID của module.
     * @return Chi tiết module.
     */
    @Override
    public ContentModuleRes getOne(UUID id) {
        ContentModule entity = contentModuleRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("ContentModule not found: " + id));
        return toRes(entity);
    }

    /**
     * Lấy phiên bản mới nhất của module dựa trên mã code và ngôn ngữ.
     *
     * @param code Mã code của module.
     * @param lang Ngôn ngữ.
     * @return Chi tiết phiên bản mới nhất.
     */
    @Override
    public ContentModuleRes getLatestByCode(String code, String lang) {
        String normalizedLang = normalizeLang(lang);
        ContentModule entity = contentModuleRepo
                .findTopByCodeAndLangOrderByVersionDesc(code.trim(), normalizedLang)
                .orElseThrow(() -> new NotFoundException(
                        "ContentModule not found for code=" + code + ", lang=" + normalizedLang));
        return toRes(entity);
    }

    /**
     * Lấy danh sách tất cả các phiên bản của một module.
     *
     * @param code Mã code của module.
     * @param lang Ngôn ngữ.
     * @return Danh sách các phiên bản.
     */
    @Override
    public List<ContentModuleRes> listVersions(String code, String lang) {
        String normalizedLang = normalizeLang(lang);
        return contentModuleRepo
                .findByCodeAndLangOrderByVersionDesc(code.trim(), normalizedLang)
                .stream()
                .map(this::toRes)
                .toList();
    }

    /**
     * Tìm kiếm module theo từ khóa trong mã code.
     *
     * @param codeKeyword Từ khóa tìm kiếm.
     * @param lang        Ngôn ngữ.
     * @param pageable    Thông tin phân trang.
     * @return Trang kết quả tìm kiếm.
     */
    @Override
    public Page<ContentModuleRes> search(String codeKeyword, String lang, Pageable pageable) {
        String normalizedLang = normalizeLang(lang);
        String keyword = (codeKeyword == null ? "" : codeKeyword.trim());

        return contentModuleRepo
                .findByCodeContainingIgnoreCaseAndLang(keyword, normalizedLang, pageable)
                .map(this::toRes);
    }

    /**
     * Chuẩn hóa mã ngôn ngữ (mặc định là 'vi' nếu null hoặc rỗng).
     */
    private String normalizeLang(String lang) {
        String value = (lang == null || lang.isBlank()) ? "vi" : lang.trim();
        return value.toLowerCase(Locale.ROOT);
    }

    /**
     * Chuyển đổi từ entity sang DTO phản hồi.
     */
    private ContentModuleRes toRes(ContentModule m) {
        return new ContentModuleRes(
                m.getId(),
                m.getCode(),
                m.getType(),
                m.getLang(),
                m.getVersion(),
                m.getPayload(),
                m.getUpdatedAt(),
                buildEtag(m)
        );
    }
    /**
     * Tạo ETag để hỗ trợ caching phía client.
     */
    private String buildEtag(ContentModule m) {
        // Công thức đơn giản nhưng ổn định, bạn có thể đổi sau
        return m.getCode() + ":" + m.getLang() + ":" + m.getVersion() + ":" +
                (m.getUpdatedAt() != null ? m.getUpdatedAt().toInstant().toEpochMilli() : "0");
    }

}
