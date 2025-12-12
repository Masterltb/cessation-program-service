package com.smokefree.program.domain.service;



import com.smokefree.program.web.dto.module.ContentModuleCreateReq;
import com.smokefree.program.web.dto.module.ContentModuleRes;
import com.smokefree.program.web.dto.module.ContentModuleUpdateReq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import java.util.UUID;

/**
 * Service quản lý các module nội dung (Content Modules).
 * Cung cấp các chức năng để tạo, cập nhật (quản lý phiên bản), xóa và truy vấn nội dung.
 */
public interface ContentModuleService {

    /**
     * Tạo mới một module nội dung.
     *
     * @param req DTO chứa thông tin tạo mới.
     * @return Thông tin module vừa tạo.
     */
    ContentModuleRes create(ContentModuleCreateReq req);

    /**
     * Cập nhật nội dung của một module (thường sẽ tạo ra một phiên bản mới).
     *
     * @param id  ID của module gốc.
     * @param req DTO chứa thông tin cập nhật.
     * @return Thông tin module sau khi cập nhật (phiên bản mới).
     */
    ContentModuleRes update(UUID id, ContentModuleUpdateReq req);

    /**
     * Xóa một module cụ thể theo ID.
     *
     * @param id ID của module cần xóa.
     */
    void delete(UUID id);

    /**
     * Lấy chi tiết một module theo ID.
     *
     * @param id ID của module.
     * @return Chi tiết module.
     */
    ContentModuleRes getOne(UUID id);

    /**
     * Lấy phiên bản mới nhất của module dựa trên mã code và ngôn ngữ.
     *
     * @param code Mã code của module.
     * @param lang Ngôn ngữ.
     * @return Chi tiết phiên bản mới nhất.
     */
    ContentModuleRes getLatestByCode(String code, String lang);

    /**
     * Lấy danh sách tất cả các phiên bản của một module dựa trên mã code.
     *
     * @param code Mã code của module.
     * @param lang Ngôn ngữ.
     * @return Danh sách các phiên bản.
     */
    List<ContentModuleRes> listVersions(String code, String lang);

    /**
     * Tìm kiếm module theo từ khóa trong mã code.
     *
     * @param codeKeyword Từ khóa tìm kiếm.
     * @param lang        Ngôn ngữ.
     * @param pageable    Thông tin phân trang.
     * @return Trang kết quả tìm kiếm.
     */
    Page<ContentModuleRes> search(String codeKeyword, String lang, Pageable pageable);
}
