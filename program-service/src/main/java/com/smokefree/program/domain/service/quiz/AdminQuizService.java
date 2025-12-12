package com.smokefree.program.domain.service.quiz;

import com.smokefree.program.domain.model.QuizTemplate;
import com.smokefree.program.web.dto.quiz.admin.CreateFullQuizReq;
import com.smokefree.program.web.dto.quiz.admin.UpdateFullQuizReq;

import java.util.List;
import java.util.UUID;

/**
 * Service quản lý các mẫu câu hỏi (Quiz Template) dành cho Admin.
 * Cung cấp các chức năng tạo, sửa, xóa, xuất bản và lưu trữ bài kiểm tra.
 */
public interface AdminQuizService {

    /**
     * Tạo một mẫu câu hỏi mới với đầy đủ thông tin (câu hỏi, lựa chọn).
     *
     * @param req DTO chứa thông tin tạo mới.
     * @return Mẫu câu hỏi đã được tạo.
     */
    QuizTemplate createFullQuiz(CreateFullQuizReq req);

    /**
     * Cập nhật toàn bộ thông tin của một mẫu câu hỏi (chỉ áp dụng cho trạng thái DRAFT).
     *
     * @param templateId ID của mẫu câu hỏi.
     * @param req        DTO chứa thông tin cập nhật.
     */
    void updateFullQuiz(UUID templateId, UpdateFullQuizReq req);

    /**
     * Xuất bản mẫu câu hỏi, chuyển trạng thái sang PUBLISHED.
     *
     * @param templateId ID của mẫu câu hỏi.
     */
    void publishTemplate(UUID templateId);

    /**
     * Lưu trữ mẫu câu hỏi, chuyển trạng thái sang ARCHIVED.
     *
     * @param templateId ID của mẫu câu hỏi.
     */
    void archiveTemplate(UUID templateId);

    /**
     * Lấy danh sách tất cả các mẫu câu hỏi.
     *
     * @return Danh sách các mẫu câu hỏi.
     */
    List<QuizTemplate> listAll();

    /**
     * Lấy chi tiết một mẫu câu hỏi bao gồm cả câu hỏi và lựa chọn.
     *
     * @param templateId ID của mẫu câu hỏi.
     * @return Chi tiết mẫu câu hỏi.
     */
    QuizTemplate getDetail(UUID templateId);

    /**
     * Xóa một mẫu câu hỏi (chỉ khi chưa được sử dụng).
     *
     * @param templateId ID của mẫu câu hỏi.
     */
    void deleteTemplate(UUID templateId);

    /**
     * Chuyển đổi trạng thái từ PUBLISHED về DRAFT để chỉnh sửa lại.
     *
     * @param templateId ID của mẫu câu hỏi.
     */
    void revertToDraft(UUID templateId);
    /**
     * Cập nhật lại nội dung (câu hỏi/lựa chọn), giữ nguyên metadata.
     *
     * @param templateId ID của mẫu câu hỏi.
     * @param req        DTO chứa nội dung cập nhật.
     */
    void updateContent(UUID templateId, com.smokefree.program.web.dto.quiz.admin.UpdateQuizContentReq req);
}
