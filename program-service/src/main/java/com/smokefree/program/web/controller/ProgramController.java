package com.smokefree.program.web.controller;

import com.smokefree.program.domain.service.ProgramService;
import com.smokefree.program.web.dto.program.*;
import com.smokefree.program.web.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Controller quản lý các chương trình cai thuốc (Program) của người dùng.
 * Cung cấp các API để tạo mới, lấy chương trình đang hoạt động và xem lịch sử các chương trình.
 */
@RestController
@RequestMapping("/v1/programs")
@RequiredArgsConstructor
public class ProgramController {
    private final ProgramService programService;

    /**
     * Tạo một chương trình cai thuốc mới cho người dùng.
     *
     * @param userId ID người dùng (từ header).
     * @param tier   Hạng thành viên (từ header, tùy chọn).
     * @param req    Thông tin yêu cầu tạo chương trình (ngày bắt đầu, số điếu thuốc, v.v.).
     * @return Thông tin chương trình vừa được tạo.
     */
    @PostMapping
    public ProgramRes create(@RequestHeader("X-User-Id") UUID userId,
                             @RequestHeader(value="X-User-Tier", required=false) String tier,
                             @RequestBody CreateProgramReq req) {
        return programService.createProgram(userId, req, tier);
    }

    /**
     * Lấy thông tin chương trình đang hoạt động (Active) của người dùng hiện tại.
     *
     * @param userId   ID người dùng.
     * @param entState Trạng thái quyền lợi (từ header, ví dụ: TRIALING, ACTIVE).
     * @param entExp   Thời gian hết hạn quyền lợi (từ header).
     * @param tier     Hạng thành viên.
     * @return Chi tiết chương trình đang hoạt động.
     */
    @GetMapping("/active")
    public ProgramRes getActive(@RequestHeader("X-User-Id") UUID userId,
                                @RequestHeader(value="X-Ent-State", required=false) String entState,
                                @RequestHeader(value="X-Ent-ExpiresAt", required=false) String entExp,
                                @RequestHeader(value="X-User-Tier", required=false) String tier) {
        var p = programService.getActive(userId).orElseThrow(() -> new NotFoundException("No ACTIVE program"));
        return programService.toRes(p, entState, entExp == null ? null : Instant.parse(entExp), tier);
    }

    /**
     * Lấy danh sách tất cả các chương trình của người dùng (bao gồm cả lịch sử).
     *
     * @param userId ID người dùng.
     * @param tier   Hạng thành viên.
     * @return Danh sách các chương trình.
     */
    @GetMapping
    public List<ProgramRes> listByUser(@RequestHeader("X-User-Id") UUID userId,
                                       @RequestHeader(value="X-User-Tier", required=false) String tier) {
        return programService.listByUser(userId).stream()
                .map(p -> programService.toRes(p, null, null, tier))
                .toList();
    }
}