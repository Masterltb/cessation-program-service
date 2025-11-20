package com.smokefree.program.domain.service;

import com.smokefree.program.web.dto.enrollment.EnrollmentRes;
import com.smokefree.program.web.dto.enrollment.StartEnrollmentReq;

import java.util.List;
import java.util.UUID;

public interface EnrollmentService {
    EnrollmentRes startTrialOrPaid(UUID userId, StartEnrollmentReq req);
    List<EnrollmentRes> listByUser(UUID userId);
    void complete(UUID userId, UUID enrollmentId);

}
