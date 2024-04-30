package searchengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "indexing-settings")
@Component
public class SitesList {
    private List<Site> sites;
}
