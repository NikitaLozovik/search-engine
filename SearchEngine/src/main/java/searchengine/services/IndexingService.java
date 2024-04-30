package searchengine.services;

import java.util.Map;

public interface IndexingService {
    Map<String, String> startIndexing();

    Map<String, String> stopIndexing();

    Map<String, String> indexPage(String url);
}
