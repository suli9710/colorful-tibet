package com.tibet.tourism.security.antibot;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AntibotProperties.class)
public class AntibotConfig {
}
