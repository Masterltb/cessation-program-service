
package com.smokefree.program.domain.model;

/**
 * Enum định nghĩa các loại sự kiện liên quan đến hành vi hút thuốc hoặc quy trình phục hồi.
 * <p>
 * Được sử dụng để phân loại các log sự kiện trong hệ thống, giúp theo dõi
 * lịch sử tái nghiện và nỗ lực phục hồi của người dùng.
 * </p>
 */
public enum SmokeEventType {
    /** Do người dùng báo cáo một lần hút thuốc (có thể là lỡ hút - Slip hoặc tái nghiện - Relapse). */
    SMOKE,

    /** (Dự phòng) Đánh dấu thời điểm người dùng bắt đầu làm nhiệm vụ phục hồi. */
    RECOVERY_START,

    /** (Dự phòng) Đánh dấu người dùng đã hoàn thành nhiệm vụ phục hồi thành công. */
    RECOVERY_SUCCESS,

    /** (Dự phòng) Đánh dấu người dùng thất bại trong nhiệm vụ phục hồi hoặc bỏ cuộc. */
    RECOVERY_FAIL
}
