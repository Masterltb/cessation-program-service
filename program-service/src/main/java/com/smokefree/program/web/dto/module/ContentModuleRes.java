// src/main/java/com/smokefree/program/web/dto/module/ContentModuleRes.java
package com.smokefree.program.web.dto.module;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record ContentModuleRes(
        UUID id,
        String code,
        String type,
        String lang,
        Integer version,
        Map<String, Object> payload,
        OffsetDateTime updatedAt,
        String etag
) {}
