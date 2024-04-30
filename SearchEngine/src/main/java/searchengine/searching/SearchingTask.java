package searchengine.searching;

import lombok.RequiredArgsConstructor;
import searchengine.model.*;
import searchengine.repositories.*;
import java.util.*;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SearchingTask extends RecursiveTask<List<PageEntity>> {
    private final SiteEntity siteEntity;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final Map<String, Integer> lemmaRankMap;
    private final double maxLemmaPercentage;
    private final Map<PageEntity, Double> pageRelevanceMap;

    @Override
    protected List<PageEntity> compute() {
        long totalPageCount = pageRepository.countBySite(siteEntity);
        List<LemmaEntity> lemmaEntities = lemmaRankMap.keySet().stream()
                .map(lemma -> lemmaRepository.findOneBySiteAndLemma(siteEntity, lemma))
                .filter(Objects::nonNull)
                .filter(lemmaEntity ->
                        ((double) lemmaEntity.getFrequency() / totalPageCount)
                                <= maxLemmaPercentage)
                .sorted(Comparator.comparing(LemmaEntity::getFrequency))
                .toList();

        if (lemmaEntities.isEmpty()) {
            return new ArrayList<>();
        }

        LemmaEntity firstLemma = lemmaEntities.get(0);
        List<IndexEntity> indexEntities = indexRepository.findByLemma(firstLemma);
        List<PageEntity> pageEntities = indexEntities.stream()
                .map(IndexEntity::getPage)
                .collect(Collectors.toList());

        for (int i = 1; i < lemmaEntities.size(); i++) {
            LemmaEntity lemmaEntity = lemmaEntities.get(i);
            List<LemmaEntity> remainingLemmas = lemmaEntities.subList(i, lemmaEntities.size());

            pageEntities = pageEntities.stream()
                    .filter(pageEntity -> indexRepository.findByLemmaAndPage(lemmaEntity, pageEntity) != null)
                    .filter(pageEntity -> remainingLemmas.stream()
                            .allMatch(lemma -> indexRepository.findByLemmaAndPage(lemma, pageEntity) != null))
                    .toList();
        }

        for(PageEntity pageEntity : pageEntities) {
            double pageRel = lemmaEntities.stream()
                    .mapToDouble(lemmaEntity ->
                            indexRepository.findByLemmaAndPage(lemmaEntity, pageEntity).getRank()
                    )
                    .sum();
            pageRelevanceMap.put(pageEntity, pageRel);
        }

        return pageEntities;
    }
}
