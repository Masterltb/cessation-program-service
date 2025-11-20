package com.smokefree.program.config;

import com.smokefree.program.auth.DevAutoUserFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class
DevOnlyBeans {
    @Bean
    public DevAutoUserFilter devAutoUserFilter() {
        return new DevAutoUserFilter();
    }
}
