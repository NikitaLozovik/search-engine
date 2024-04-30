package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Data
@Table(name = "lemma")
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @JoinColumn(name = "site_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private SiteEntity site;

    @Column(name = "lemma")
    private String lemma;

    @Column(name = "frequency")
    private Integer frequency;

    @OneToMany(mappedBy = "lemma", fetch = FetchType.LAZY)
    private List<IndexEntity> indexes;

    public synchronized void incrementFrequency() {
        frequency++;
    }
}
