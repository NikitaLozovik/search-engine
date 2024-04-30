package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Entity
@Data
@Table(name = "site")
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "status_time")
    private Instant statusTime;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "url")
    private String url;

    @Column(name = "name")
    private String name;

    @OneToMany(mappedBy = "site", fetch = FetchType.LAZY)
    private List<PageEntity> pages;

    @OneToMany(mappedBy = "site", fetch = FetchType.LAZY)
    private List<LemmaEntity> lemmas;
}
