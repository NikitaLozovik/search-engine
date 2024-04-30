package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
@Table(name = "page")
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @JoinColumn(name = "site_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private SiteEntity site;

    @Column(name = "path")
    private String path;

    @Column(name = "code")
    private Integer code;

    @Column(name = "title")
    private String title;

    @Column(name = "content")
    private String content;

    @OneToMany(mappedBy = "page", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private List<IndexEntity> indexes;
}
