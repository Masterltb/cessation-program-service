package com.smokefree.program.web.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Profile("dev")
@RestController
@RequestMapping("/debug")
public class DebugController {
    @GetMapping("/whoami")
    public Map<String, Object> whoami(Authentication auth) {
        return Map.of(
                "principal", auth == null ? null : auth.getName(),
                "roles", auth == null ? List.of() : auth.getAuthorities()
        );
    }
}


