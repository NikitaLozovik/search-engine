package searchengine.services;

import org.springframework.context.annotation.*;
import searchengine.indexing.*;
import java.util.List;
import java.util.concurrent.*;

@Configuration
public class IndexingServiceConfig {
    @Bean
    public List<IndexingTask> tasks() {
        return new CopyOnWriteArrayList<>();
    }
}
