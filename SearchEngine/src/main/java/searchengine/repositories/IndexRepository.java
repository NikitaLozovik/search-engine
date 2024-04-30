package searchengine.repositories;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.*;
import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {
    @Query
    List<IndexEntity> findByLemma(LemmaEntity lemma);

    @Query
    IndexEntity findByLemmaAndPage(LemmaEntity lemma, PageEntity page);

    @Query
    void deleteAllByPage(PageEntity page);

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE `index`", nativeQuery = true)
    void truncateTable();
}
