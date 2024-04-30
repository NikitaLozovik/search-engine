package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "`index`")
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @JoinColumn(name = "lemma_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private LemmaEntity lemma;

    @JoinColumn(name = "page_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private PageEntity page;

    @Column(name = "`rank`")
    private Double rank;
}
