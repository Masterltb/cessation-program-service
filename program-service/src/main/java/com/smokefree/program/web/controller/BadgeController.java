package com.smokefree.program.web.controller;

import com.smokefree.program.domain.model.Badge;
import com.smokefree.program.domain.model.UserBadge;
import com.smokefree.program.domain.repo.BadgeRepository;
import com.smokefree.program.domain.repo.UserBadgeRepository;
import com.smokefree.program.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/me/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final UserBadgeRepository userBadgeRepository;
    private final BadgeRepository badgeRepository;

    /**
     * Lấy danh sách huy hiệu user đã đạt được.
     */
    @GetMapping

    public List<MyBadgeRes> getMyBadges() {
        UUID userId = SecurityUtil.requireUserId();
        
        // Lấy tất cả user badges (Join fetch badge)
        List<UserBadge> earned = userBadgeRepository.findAllByUserIdWithBadge(userId);
        
        // Map sang DTO
        return earned.stream()
                .map(ub -> new MyBadgeRes(
                        ub.getBadge().getCode(),
                        ub.getBadge().getCategory(),
                        ub.getBadge().getLevel(),
                        ub.getBadge().getName(),
                        ub.getBadge().getDescription(),
                        ub.getBadge().getIconUrl(),
                        ub.getEarnedAt()
                ))
                .toList();
    }

    /**
     * Lấy danh mục toàn bộ huy hiệu hệ thống (để hiển thị UI dạng lưới, ẩn/hiện).
     */
    @GetMapping("/all")

    public Map<String, List<BadgeDefRes>> getAllBadges() {
        // Group by Category
        return badgeRepository.findAll().stream()
                .map(b -> new BadgeDefRes(
                        b.getCode(), b.getCategory(), b.getLevel(),
                        b.getName(), b.getDescription(), b.getIconUrl()
                ))
                .collect(Collectors.groupingBy(BadgeDefRes::category));
    }

    public record MyBadgeRes(
        String code, String category, int level,
        String name, String description, String iconUrl,
        Instant earnedAt
    ) {}

    public record BadgeDefRes(
        String code, String category, int level,
        String name, String description, String iconUrl
    ) {}
}
