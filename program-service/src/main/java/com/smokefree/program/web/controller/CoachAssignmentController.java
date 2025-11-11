package com.smokefree.program.web.controller;

import com.smokefree.program.domain.service.CoachAssignmentService;
import com.smokefree.program.web.dto.coach.AssignReq;
import com.smokefree.program.web.dto.coach.CoachAssignmentRes;
import com.smokefree.program.web.dto.coach.MyCustomerRes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Void> unassign(@PathVariable UUID id) {
        service.unassign(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-customers")
    @PreAuthorize("hasRole('COACH')")
    public List<MyCustomerRes> myCustomers() {
        UUID coachId = com.smokefree.program.util.SecurityUtil.requireUserId();
        return service.listCustomers(coachId);
    }
}

