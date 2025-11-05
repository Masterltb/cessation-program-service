package com.smokefree.program.web.controller;

import com.smokefree.program.domain.service.CoachAssignmentService;
import com.smokefree.program.web.dto.coach.AssignReq;
import com.smokefree.program.web.dto.coach.CoachAssignmentRes;
import com.smokefree.program.web.dto.coach.MyCustomerRes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coach-assignments")
public class CoachAssignmentController {

    private final CoachAssignmentService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    public CoachAssignmentRes assign(@RequestBody @Valid AssignReq req) {
        return service.assign(req.coachId(), req.customerId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    public void unassign(@PathVariable UUID id) {
        service.unassign(id);
    }

    @GetMapping("/my-customers")
    @PreAuthorize("hasRole('COACH')")
    public List<MyCustomerRes> myCustomers() {
        // coachId lấy từ SecurityContext nếu bạn dùng UserPrincipal; tạm để client gọi /admin lấy
        // TODO: thay bằng SecurityUtil.currentUserId() nếu principal là coach
        return List.of();
    }
}
