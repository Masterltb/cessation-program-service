package com.smokefree.program.domain.model;

public enum QuizTemplateScope {
    SYSTEM,   // template hệ thống do admin tạo (ownerId = null)
    COACH     // template của coach (ownerId = coachId)
}
