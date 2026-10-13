package com.example.chronos.config;

import com.example.chronos.support.UrlSafetyValidator;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfig {

    @Bean
    UrlSafetyValidator urlSafetyValidator(@Value("${chronos.security.allow-private-targets:false}") boolean allowPrivate) {
        return new UrlSafetyValidator(allowPrivate);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
