package com.smokefree.program.web.controller;


import com.smokefree.program.domain.service.ContentModuleService;
import com.smokefree.program.web.dto.module.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
public class ModuleController {

    private final ContentModuleService contentModuleService;

    // --- CRUD cơ bản cho admin/content team ---

    @PostMapping
    // tuỳ bạn cấu hình AuthorizationHelper
    public ResponseEntity<ContentModuleRes> create(
            @Valid @RequestBody ContentModuleCreateReq req
    ) {
        ContentModuleRes res = contentModuleService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContentModuleRes> update(
            @PathVariable UUID id,
            @Valid @RequestBody ContentModuleUpdateReq req
    ) {
        ContentModuleRes res = contentModuleService.update(id, req);
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        contentModuleService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    // hoặc mở rộng thêm
    public ResponseEntity<ContentModuleRes> getOne(@PathVariable UUID id) {
        ContentModuleRes res = contentModuleService.getOne(id);
        return ResponseEntity.ok(res);
    }

    // --- Endpoint phục vụ FE / Program / Step ---

    /**
     * Lấy module mới nhất theo code + lang.
     * Lang có thể đọc từ header Accept-Language hoặc query param.
     */
    @GetMapping("/by-code/{code}")
    public ResponseEntity<ContentModuleRes> getLatestByCode(
            @PathVariable String code,
            @RequestParam(name = "lang", required = false) String lang
    ) {
        ContentModuleRes res = contentModuleService.getLatestByCode(code, lang);
        return ResponseEntity.ok(res);
    }

    /**
     * Xem tất cả version của 1 module để debug hoặc admin xem lịch sử.
     */
    @GetMapping("/by-code/{code}/versions")
    public ResponseEntity<List<ContentModuleRes>> listVersions(
            @PathVariable String code,
            @RequestParam(name = "lang", required = false) String lang
    ) {
        List<ContentModuleRes> list = contentModuleService.listVersions(code, lang);
        return ResponseEntity.ok(list);
    }

    /**
     * Search module theo code keyword, có phân trang.
     */
    @GetMapping
    public ResponseEntity<Page<ContentModuleRes>> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "lang", required = false) String lang,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ContentModuleRes> result = contentModuleService.search(q, lang, pageable);
        return ResponseEntity.ok(result);
    }
}