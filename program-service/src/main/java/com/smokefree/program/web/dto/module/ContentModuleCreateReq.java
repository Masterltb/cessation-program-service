package com.smokefree.program.web.dto.module;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ContentModuleCreateReq(
        @NotBlank String code,
        @NotBlank String type,
        String lang,              // cho phép null -> default "vi"
        Integer version,          // cho phép null -> auto next version
        @NotNull Map<String, Object> payload
) {}
