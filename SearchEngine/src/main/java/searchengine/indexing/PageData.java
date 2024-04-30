package searchengine.indexing;

import lombok.Data;
import org.jsoup.nodes.Document;

@Data
public class PageData {
    private Document document;
    private int statusCode;
}
