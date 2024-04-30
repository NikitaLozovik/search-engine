package searchengine.repositories;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.*;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    @Query
    long countBySite(SiteEntity site);

    @Query
    PageEntity findOneBySiteAndPath(SiteEntity site, String path);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM `page`", nativeQuery = true)
    void deleteAllRecords();

    @Modifying
    @Transactional
    @Query(value = "ALTER TABLE `page` AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
}
