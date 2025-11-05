package com.smokefree.program.domain.service;


import com.smokefree.program.domain.service.CoachAssignmentService;
import com.smokefree.program.web.dto.coach.CoachAssignmentRes;
import com.smokefree.program.web.dto.coach.MyCustomerRes;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CoachAssignmentServiceImpl implements CoachAssignmentService {
    @Override
    public CoachAssignmentRes assign(UUID coachId, UUID customerId) {
        // TODO: kiểm tra coach < 3 khách
        return new CoachAssignmentRes(UUID.randomUUID(), coachId, customerId, Instant.now());
    }

    @Override
    public void unassign(UUID assignmentId) {
        // TODO: xóa record
    }

    @Override
    public List<MyCustomerRes> listCustomers(UUID coachId) {
        // TODO: query DB
        return List.of();
    }
}