package searchengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "indexing-settings")
@Component
public class Config {
    private String userAgent;
    private String referrer;
    private Integer timeOut;
    private Long delay;
    private Long threshold;
    private Double maxLemmaOccurrencePercentage;
}
