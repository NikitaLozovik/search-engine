package searchengine.repositories;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteEntity;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    @Query
    SiteEntity findOneByUrl(String url);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM `site`", nativeQuery = true)
    void deleteAllRecords();

    @Modifying
    @Transactional
    @Query(value = "ALTER TABLE `site` AUTO_INCREMENT = 1", nativeQuery = true)
    void resetAutoIncrement();
}
