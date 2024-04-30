package searchengine.indexing;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class CancelFlag {
    private volatile boolean isCancelled;
}
