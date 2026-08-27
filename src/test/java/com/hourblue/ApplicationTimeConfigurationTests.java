package com.hourblue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "APP_TIME_ZONE=Asia/Kolkata")
class ApplicationTimeConfigurationTests {

    @Autowired
    private ZoneId applicationZoneId;

    @Test
    void timeZoneCanBeConfiguredExternally() {
        assertEquals(ZoneId.of("Asia/Kolkata"), applicationZoneId);
    }
}