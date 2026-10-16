package kz.jyly.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    /** Сервистер уақытты осы Clock арқылы алады — тесттерде уақытты бекітуге болады. */
    @Bean
    public Clock clock(JylyProperties properties) {
        return Clock.system(ZoneId.of(properties.getZone()));
    }
}
