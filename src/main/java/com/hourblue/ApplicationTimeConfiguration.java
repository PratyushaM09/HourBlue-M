package com.hourblue;

import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ApplicationTimeConfiguration {

    @Bean
    ZoneId applicationZoneId(
            @Value("${hourblue.time-zone}") String timeZone) {
        return ZoneId.of(timeZone);
    }
}