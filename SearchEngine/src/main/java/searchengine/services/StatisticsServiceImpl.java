package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.*;
import searchengine.model.SiteEntity;
import searchengine.repositories.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexingServiceImpl indexingService;

    @Override
    public StatisticsResponse getStatistics() {
        List<SiteEntity> siteEntities = siteRepository.findAll();

        TotalStatistics total = new TotalStatistics();
        total.setSites(siteEntities.size());
        total.setPages((int) pageRepository.count());
        total.setLemmas((int) lemmaRepository.count());
        total.setIndexing(indexingService.isIndexing());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for(SiteEntity siteEntity : siteEntities) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            String error = siteEntity.getLastError();
            item.setName(siteEntity.getName());
            item.setUrl(siteEntity.getUrl());
            item.setPages((int) pageRepository.countBySite(siteEntity));
            item.setLemmas((int) lemmaRepository.countBySite(siteEntity));
            item.setStatus(siteEntity.getStatus().toString());
            item.setError(error == null ? "" : error);
            item.setStatusTime(siteEntity.getStatusTime().toEpochMilli());
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
