package com.smokefree.program.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Lớp cấu hình ánh xạ các thuộc tính bắt đầu bằng tiền tố "app" từ file cấu hình.
 * Dùng để quản lý các tham số cấu hình chung của ứng dụng.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProps {
    /**
     * Thời gian UTC (định dạng HH:mm) để gửi thông báo nhắc nhở hàng ngày.
     * Mặc định là 00:30.
     */
    private String notifDailyUtc = "00:30";

    /**
     * Lấy giá trị thời gian thông báo hàng ngày.
     *
     * @return Chuỗi thời gian (HH:mm).
     */
    public String getNotifDailyUtc() { return notifDailyUtc; }

    /**
     * Cập nhật giá trị thời gian thông báo hàng ngày.
     *
     * @param v Chuỗi thời gian mới (HH:mm).
     */
    public void setNotifDailyUtc(String v) { this.notifDailyUtc = v; }
}
