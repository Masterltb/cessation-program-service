package com.smokefree.program.domain.model;

/**
 * Đại diện cho trạng thái vòng đời của một Mẫu câu hỏi (Quiz Template).
 */
public enum QuizTemplateStatus {
    /**
     * Bài kiểm tra hiện đang được thiết kế và chưa hiển thị với người dùng (Bản nháp).
     */
    DRAFT,

    /**
     * Bài kiểm tra đang hoạt động và sẵn sàng cho người dùng tham gia (Đã xuất bản).
     */
    PUBLISHED,

    /**
     * Bài kiểm tra đã ngừng hoạt động và không còn cho phép lượt thi mới,
     * nhưng được giữ lại cho hồ sơ lịch sử (Đã lưu trữ).
     */
    ARCHIVED
}