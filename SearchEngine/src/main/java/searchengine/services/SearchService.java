package searchengine.services;

import searchengine.dto.search.SearchResponse;

public interface SearchService {
    SearchResponse search(String query, Integer offset, Integer limit, String site);
}
