package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.*;
import searchengine.indexing.*;
import searchengine.lemma.Lemmatizer;
import searchengine.model.*;
import searchengine.repositories.*;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService{
    private final SitesList sites;
    private final Config config;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final List<IndexingTask> tasks;
    private final CancelFlag cancelFlag;
    private final Lemmatizer lemmatizer;
    private final Parser parser;

    private static final String CANCELED_MESSAGE;
    private static final String RUNNING_MESSAGE;
    private static final String NOT_RUNNING_MESSAGE;
    private static final String WRONG_PAGE_MESSAGE;
    private static final String PAGE_DATA_FETCH_FAILED;

    static {
        CANCELED_MESSAGE = "Индексация остановлена пользователем";
        RUNNING_MESSAGE = "Индексация уже запущена";
        NOT_RUNNING_MESSAGE = "Индексация не запущена";
        WRONG_PAGE_MESSAGE = "Данная страница находится за пределами сайтов, " +
                "указанных в конфигурационном файле";
        PAGE_DATA_FETCH_FAILED = "Не удалось получить данные страницы";
    }

    @Override
    @Transactional
    public Map<String, String> startIndexing() {
        Map<String, String> response = new HashMap<>();
        CountDownLatch latch = new CountDownLatch(1);

        if(!tasks.isEmpty()) {
            response.put("result", "false");
            response.put("error", RUNNING_MESSAGE);
            return response;
        }

        cancelFlag.setCancelled(false);

        new Thread(() -> {
            deleteAllData();
            latch.countDown();
        }).start();

        sites.getSites().forEach(site -> {
            Map<String, String> visited = new ConcurrentHashMap<>();
            Map<String, LemmaEntity> lemmas = new HashMap<>();
            Semaphore semaphore = new Semaphore(1);
            String url = site.getUrl();
            String name = site.getName();

            new Thread(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                visited.put("/", "");
                SiteEntity siteEntity = new SiteEntity();
                siteEntity.setUrl(url);
                siteEntity.setName(name);
                siteEntity.setStatus(Status.INDEXING);
                siteEntity.setStatusTime(Instant.now());
                siteRepository.save(siteEntity);

                IndexingTask task = new IndexingTask(
                        this,
                        config, url, siteRepository.findOneByUrl(url), lemmatizer,
                        siteRepository,
                        visited, lemmas, cancelFlag, parser, 0, semaphore
                );
                tasks.add(task);
                List<PageEntity> pages = task.invoke();
                tasks.remove(task);

                if(pages == null) {
                    return;
                }

                siteEntity = siteRepository.findOneByUrl(url);
                siteEntity.setStatus(cancelFlag.isCancelled() ? Status.FAILED : Status.INDEXED);
                siteEntity.setLastError(cancelFlag.isCancelled() ? CANCELED_MESSAGE : null);
                siteEntity.setStatusTime(Instant.now());
                siteRepository.save(siteEntity);

                log.info(String.format("Site %s indexed", site.getName()));
            }).start();
        });
        response.put("result", "true");
        return response;
    }

    @Override
    public Map<String, String> stopIndexing() {
        Map<String, String> response = new HashMap<>();
        if(tasks.isEmpty()) {
            response.put("result", "false");
            response.put("error", NOT_RUNNING_MESSAGE);
            return response;
        }
        cancelFlag.setCancelled(true);
        response.put("result", "true");
        return response;
    }

    public boolean isIndexing() {
        return !tasks.isEmpty();
    }

    @Override
    @Transactional
    public Map<String, String> indexPage(String url) {
        HashMap<String, String> response = new HashMap<>();
        SiteEntity siteEntity;
        PageEntity pageEntity;
        Map<String, Integer> lemmaMap;
        List<LemmaEntity> lemmaEntities = new ArrayList<>();
        List<IndexEntity> indexEntities = new ArrayList<>();
        Site site = null;
        for(Site s : sites.getSites()) {
            if(url.startsWith(s.getUrl())) {
                site = s;
            }
        }
        if(site == null) {
            response.put("result", "false");
            response.put("error", WRONG_PAGE_MESSAGE);
            return response;
        }

        siteEntity = siteRepository.findOneByUrl(site.getUrl());
        if(siteEntity == null) {
            siteEntity = new SiteEntity();
            siteEntity.setStatus(Status.INDEXED);
            siteEntity.setStatusTime(Instant.now());
            siteEntity.setLastError("");
            siteEntity.setUrl(site.getUrl());
            siteEntity.setName(site.getName());
            siteEntity = siteRepository.save(siteEntity);
        }
        if(siteEntity.getStatus() == Status.INDEXING) {
            response.put("result", "false");
            response.put("error", RUNNING_MESSAGE);
            return response;
        }

        pageEntity = pageRepository.findOneBySiteAndPath(siteEntity, pathFromRoot(site.getUrl(), url));
        if(pageEntity != null) {
            indexRepository.deleteAllByPage(pageEntity);
            pageRepository.deleteById(pageEntity.getId());
            pageEntity = new PageEntity();
            pageEntity.setSite(siteEntity);
            pageEntity.setPath(pathFromRoot(site.getUrl(), url));
        }

        pageEntity = new PageEntity();
        pageEntity.setSite(siteEntity);
        pageEntity.setPath(pathFromRoot(site.getUrl(), url));

        PageData pageData;
        try {
            pageData = parser.getConnection(url);
        } catch (IOException e) {
            response.put("result", "false");
            response.put("error", PAGE_DATA_FETCH_FAILED);
            return response;
        }

        pageEntity.setCode(pageData.getStatusCode());
        pageEntity.setContent(pageData.getDocument().text());
        pageEntity.setTitle(pageData.getDocument().title());
        pageEntity = pageRepository.save(pageEntity);

        lemmaMap = lemmatizer.lemmatizeText(pageEntity.getContent());
        for(String lemma : lemmaMap.keySet()) {
            LemmaEntity lemmaEntity = lemmaRepository.findOneBySiteAndLemma(siteEntity, lemma);
            if(lemmaEntity == null) {
                lemmaEntity = new LemmaEntity();
                lemmaEntity.setSite(siteEntity);
                lemmaEntity.setLemma(lemma);
                lemmaEntity.setFrequency(0);
            }
            lemmaEntity.incrementFrequency();
            lemmaEntities.add(lemmaEntity);

            IndexEntity indexEntity = new IndexEntity();
            indexEntity.setLemma(lemmaEntity);
            indexEntity.setRank(Double.valueOf(lemmaMap.get(lemma)));
            indexEntity.setPage(pageEntity);
            indexEntities.add(indexEntity);
        }
        lemmaRepository.saveAllAndFlush(lemmaEntities);
        indexRepository.saveAllAndFlush(indexEntities);

        response.put("result", "true");
        return response;
    }

    public void savePagesData(List<PageEntity> pages, SiteEntity siteEntity, Map<String, LemmaEntity> lemmas,
                              Semaphore semaphore) {
        List<LemmaEntity> lemmaEntities = new ArrayList<>();
        List<IndexEntity> indexEntities = new ArrayList<>();

        pages = pageRepository.saveAllAndFlush(pages);

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        for (PageEntity pageEntity : pages) {
            Map<String, Integer> lemmaMap;
            lemmaMap = lemmatizer.lemmatizeText(pageEntity.getContent());

            for (String lemma : lemmaMap.keySet()) {
                LemmaEntity lemmaEntity = lemmas.computeIfAbsent(lemma, key -> {
                    LemmaEntity newLemma = new LemmaEntity();
                    newLemma.setFrequency(0);
                    newLemma.setSite(siteEntity);
                    newLemma.setLemma(lemma);
                    return newLemma;
                });
                lemmaEntity.incrementFrequency();
                lemmaEntities.add(lemmaEntity);

                IndexEntity indexEntity = new IndexEntity();
                indexEntity.setLemma(lemmaEntity);
                indexEntity.setRank(Double.valueOf(lemmaMap.get(lemma)));
                indexEntity.setPage(pageEntity);
                indexEntities.add(indexEntity);
            }
        }

        lemmaRepository.saveAllAndFlush(lemmaEntities);
        indexRepository.saveAllAndFlush(indexEntities);
        semaphore.release();
    }

    @Transactional
    private void deleteAllData() {
        log.debug("Removing all data");
        indexRepository.truncateTable();
        lemmaRepository.deleteAllRecords();
        lemmaRepository.resetAutoIncrement();
        pageRepository.deleteAllRecords();
        pageRepository.resetAutoIncrement();
        siteRepository.deleteAllRecords();
        siteRepository.resetAutoIncrement();
    }

    public String pathFromRoot(String rootUrl, String url) {
        String path = url.replace(rootUrl, "");
        path = path.startsWith("/") ? path : "/" + path;
        path = path.endsWith("/") ? path : path + "/";
        return path;
    }
}
