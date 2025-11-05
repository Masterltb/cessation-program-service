package com.smokefree.program.domain.service;

import com.smokefree.program.web.dto.coach.CoachAssignmentRes;
import com.smokefree.program.web.dto.coach.MyCustomerRes;

import java.util.List;
import java.util.UUID;

public interface CoachAssignmentService {
    CoachAssignmentRes assign(UUID coachId, UUID customerId);
    void unassign(UUID assignmentId);
    List<MyCustomerRes> listCustomers(UUID coachId);
}
