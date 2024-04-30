package searchengine.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import searchengine.config.Config;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class Parser {
    private final Config config;
    public PageData getConnection(String url) throws IOException {
        PageData pageData = new PageData();
        Document document;
        Connection.Response response;

        response = Jsoup.connect(url)
                .ignoreContentType(true)
                .userAgent(config.getUserAgent())
                .referrer(config.getReferrer())
                .timeout(config.getTimeOut())
                .followRedirects(true)
                .execute();
        document = response.parse();

        pageData.setDocument(document);
        pageData.setStatusCode(response.statusCode());
        return pageData;
    }
}
