package com.smokefree.program.domain.service;

import com.smokefree.program.domain.model.ContentModule;
import com.smokefree.program.domain.repo.ContentModuleRepo;

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

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentModuleServiceImpl implements ContentModuleService {

    private final ContentModuleRepo contentModuleRepo;

    @Override
    @Transactional
    public ContentModuleRes create(ContentModuleCreateReq req) {
        String lang = normalizeLang(req.lang());
        String code = req.code().trim();
        String type = req.type().trim();

        Integer version = req.version();
        if (version == null) {
            var latestOpt = contentModuleRepo.findTopByCodeAndLangOrderByVersionDesc(code, lang);
            version = latestOpt.map(m -> m.getVersion() + 1).orElse(1);
        }

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


    @Override
    @Transactional
    public ContentModuleRes update(UUID id, ContentModuleUpdateReq req) {
        ContentModule entity = contentModuleRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("ContentModule not found: " + id));

        entity.setType(req.type().trim());
        entity.setPayload(req.payload());
        // updatedAt sẽ được set trong @PreUpdate

        ContentModule saved = contentModuleRepo.save(entity);
        return toRes(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        ContentModule entity = contentModuleRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("ContentModule not found: " + id));
        contentModuleRepo.delete(entity);
    }

    @Override
    public ContentModuleRes getOne(UUID id) {
        ContentModule entity = contentModuleRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("ContentModule not found: " + id));
        return toRes(entity);
    }

    @Override
    public ContentModuleRes getLatestByCode(String code, String lang) {
        String normalizedLang = normalizeLang(lang);
        ContentModule entity = contentModuleRepo
                .findTopByCodeAndLangOrderByVersionDesc(code.trim(), normalizedLang)
                .orElseThrow(() -> new NotFoundException(
                        "ContentModule not found for code=" + code + ", lang=" + normalizedLang));
        return toRes(entity);
    }

    @Override
    public List<ContentModuleRes> listVersions(String code, String lang) {
        String normalizedLang = normalizeLang(lang);
        return contentModuleRepo
                .findByCodeAndLangOrderByVersionDesc(code.trim(), normalizedLang)
                .stream()
                .map(this::toRes)
                .toList();
    }

    @Override
    public Page<ContentModuleRes> search(String codeKeyword, String lang, Pageable pageable) {
        String normalizedLang = normalizeLang(lang);
        String keyword = (codeKeyword == null ? "" : codeKeyword.trim());

        return contentModuleRepo
                .findByCodeContainingIgnoreCaseAndLang(keyword, normalizedLang, pageable)
                .map(this::toRes);
    }

    private String normalizeLang(String lang) {
        String value = (lang == null || lang.isBlank()) ? "vi" : lang.trim();
        return value.toLowerCase(Locale.ROOT);
    }

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
    private String buildEtag(ContentModule m) {
        // Công thức đơn giản nhưng ổn định, bạn có thể đổi sau
        return m.getCode() + ":" + m.getLang() + ":" + m.getVersion() + ":" +
                (m.getUpdatedAt() != null ? m.getUpdatedAt().toInstant().toEpochMilli() : "0");
    }

}
