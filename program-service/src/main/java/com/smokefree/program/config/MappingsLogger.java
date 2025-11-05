package com.smokefree.program.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
class MappingsLogger {
    private final RequestMappingHandlerMapping mapping;

    @EventListener(ApplicationReadyEvent.class)
    public void logMappings() {
        mapping.getHandlerMethods().forEach((info, method) ->
                log.info("Mapped: {} -> {}", info, method.getMethod().toGenericString()));
    }
}

