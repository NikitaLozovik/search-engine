package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Config;
import searchengine.dto.search.*;
import searchengine.lemma.Lemmatizer;
import searchengine.model.*;
import searchengine.repositories.*;
import searchengine.searching.SearchingTask;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final Lemmatizer lemmatizer;
    private final Config config;

    @Override
    public SearchResponse search(String query, Integer offset, Integer limit, String site) {
        SearchResponse response = new SearchResponse();
        List<SiteEntity> siteEntities;
        List<PageEntity> foundPages = new ArrayList<>();
        Map<String, Integer> lemmas = lemmatizer.lemmatizeText(query);
        Map<PageEntity, Double> pageRelevanceMap = new HashMap<>();

        siteEntities = (site == null) ?
                siteRepository.findAll() :
                List.of(siteRepository.findOneByUrl(site));

        for(SiteEntity siteEntity : siteEntities) {
            SearchingTask task = new SearchingTask(
                    siteEntity, pageRepository, indexRepository,
                    lemmaRepository, lemmas, config.getMaxLemmaOccurrencePercentage(),
                    pageRelevanceMap
            );

            foundPages.addAll(task.invoke());
        }

        double maxRel = pageRelevanceMap.values().stream().mapToDouble(Double::valueOf).max().orElse(1.0);
        int foundCount = foundPages.size();
        pageRelevanceMap.replaceAll((e, v) -> pageRelevanceMap.get(e) / maxRel);
        foundPages = foundPages.stream()
                .sorted((page1, page2) -> pageRelevanceMap.get(page2).compareTo(pageRelevanceMap.get(page1)))
                .skip(offset)
                .limit(limit)
                .toList();

        response.setResult(true);
        response.setCount(foundCount);
        List<SearchData> dataList = new ArrayList<>();
        for(PageEntity page : foundPages) {
            SearchData data = new SearchData();
            data.setSite(page.getSite().getUrl());
            data.setSiteName(page.getSite().getName());
            data.setUri(page.getPath());
            data.setTitle(titleFormat(page.getTitle()));
            data.setSnippet(makeSnippet(page.getContent(), lemmas.keySet()));
            data.setRelevance(pageRelevanceMap.get(page));
            dataList.add(data);
        }
        response.setData(dataList);
        return response;
    }

    private String titleFormat(String title) {
        return title.length() <= 60 ? title
                : title.substring(0, 60) + "...";
    }

    private String makeSnippet(String content, Set<String> lemmas) {
        String[] words = content.split("\\s+");
        List<String> used = new ArrayList<>();
        StringBuilder result = new StringBuilder();
        boolean found = false;
        int count = 10;

        result.append("...");
        for(String word : words) {
            String normalForm = lemmatizer.getNormalForm(word);
            if(!used.contains(normalForm) && lemmas.contains(normalForm)) {
                result.append("<b>").append(word).append("</b>").append(" ");
                found = true;
                count = 10;
                used.add(normalForm);
                if(used.size() == lemmas.size()) {
                    used.clear();
                }
                continue;
            }
            if(found) {
                result.append(word).append(" ");
                count--;
                if(count == 0) {
                    found = false;
                }
            }
            if(result.length() >= 220) {
                return result.append("...").toString() ;
            }
        }

        return result.toString();
    }
}
