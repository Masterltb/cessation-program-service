package com.smokefree.program.web.dto.module;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ContentModuleUpdateReq(
        @NotBlank String type,
        @NotNull Map<String, Object> payload
) {}

