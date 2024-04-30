package searchengine.indexing;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import searchengine.config.Config;
import searchengine.lemma.Lemmatizer;
import searchengine.model.*;
import searchengine.repositories.*;
import searchengine.services.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@RequiredArgsConstructor
public class IndexingTask extends RecursiveTask<List<PageEntity>> {
    private final IndexingServiceImpl indexingService;
    private final Config config;
    private final String url;
    private final SiteEntity site;
    private final Lemmatizer lemmatizer;
    private final SiteRepository siteRepository;
    private final Map<String, String> visited;
    private final Map<String, LemmaEntity> lemmas;
    private final CancelFlag cancelFlag;
    private final Parser parser;
    private final int depth;
    private final Semaphore semaphore;

    private static final List<String> WRONG_TYPES;
    private static final String MAIN_PAGE_IS_NOT_AVAILABLE;

    static {
        WRONG_TYPES = List.of(
                "pdf", "gif", "zip", "jpg",
                "jpeg", "png", "tar", "jar",
                "gz", "svg", "ppt", "pptx", "eps",
                "xlsx", "doc"
        );
        MAIN_PAGE_IS_NOT_AVAILABLE = "Ошибка индексации: главная страница сайта не доступна.";
    }

    @Override
    protected List<PageEntity> compute() {
        log.debug(String.format("New indexing task has been launched '%s'.", site.getName()));
        List<IndexingTask> subTusks = new ArrayList<>();
        List<PageEntity> pages = new ArrayList<>();
        PageData pageData;
        Document document;

        if(cancelFlag.isCancelled()) {
            log.debug(String.format("The execution is cancelled '%s'.", site.getName()));
            return pages;
        }

        try {
            pageData = parser.getConnection(url);
            document = pageData.getDocument();
            Thread.sleep(config.getDelay());
        } catch (Exception e) {
            log.error(e.getMessage() + " Site: " + site.getName());
            if(depth == 0) {
                mainPageError();
                return null;
            }
            return pages;
        }

        pages.add(newPage(pageData.getStatusCode(), pageData.getDocument().text(), document.title()));

        if(cancelFlag.isCancelled()) {
            log.debug(String.format("The execution is cancelled '%s'.", site.getName()));
            return pages;
        }

        Elements elements = document.select("a[href]");
        for(Element element : elements) {
            String reference = element.attr("abs:href");
            if(!checkUrl(reference)) continue;
            IndexingTask task = new IndexingTask(
                    indexingService, config, reference, site, lemmatizer,
                    siteRepository, visited, lemmas, cancelFlag, parser,
                    depth+1, semaphore
            );
            task.fork();
            subTusks.add(task);
        }

        subTusks.forEach(task -> pages.addAll(task.join()));

        if((pages.size() >= config.getThreshold() || depth == 0)) {
            savePages(pages);
        }

        log.debug(String.format("Task complete. %d pages returned.", pages.size()));
        return pages;
    }

    private PageEntity newPage(int statusCode, String content, String title) {
        PageEntity page = new PageEntity();
        title = (title == null || title.isBlank()) ? content.substring(0, 60)
                : title;
        page.setSite(site);
        page.setCode(statusCode);
        page.setTitle(title);
        page.setContent(content);
        page.setPath(indexingService.pathFromRoot(site.getUrl(), url));
        log.debug("New page, path: " + page.getPath());
        return page;
    }

    private void savePages(List<PageEntity> pages) {
        if(cancelFlag.isCancelled()) return;
        indexingService.savePagesData(pages, site, lemmas, semaphore);
        pages.clear();
        updateSiteStatusTime();
    }


    private void mainPageError() {
        SiteEntity siteEntity = siteRepository.findOneByUrl(site.getUrl());
        siteEntity.setStatusTime(Instant.now());
        siteEntity.setLastError(MAIN_PAGE_IS_NOT_AVAILABLE);
        siteEntity.setStatus(Status.FAILED);
        siteRepository.save(siteEntity);
    }

    private void updateSiteStatusTime() {
        SiteEntity siteEntity = siteRepository.findOneByUrl(site.getUrl());
        siteEntity.setStatusTime(Instant.now());
        siteRepository.saveAndFlush(siteEntity);
    }

    private boolean checkUrl(String url) {
        String type = url.substring(url.lastIndexOf(".")+1)
                .replaceAll("/", "")
                .toLowerCase();
        if(!url.startsWith(site.getUrl())) {
            return false;
        }
        if(url.contains("#")) {
            return false;
        }
        if(WRONG_TYPES.contains(type)) {
            return false;
        }
        return visited.putIfAbsent(indexingService.pathFromRoot(site.getUrl(), url), "") == null;
    }
}