package searchengine.repositories;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.*;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    @Query
    LemmaEntity findOneBySiteAndLemma(SiteEntity site, String lemma);

    @Query
    long countBySite(SiteEntity site);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM `lemma`", nativeQuery = true)
    void deleteAllRecords();

    @Modifying
    @Transactional
    @Query(value = "ALTER TABLE `lemma` AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
}
