package uz.dataFin.notificationbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.dataFin.notificationbot.model.Appeals;

import java.util.Optional;

public interface AppealRepository extends JpaRepository<Appeals,Long> {

    @Query(nativeQuery = true, value ="SELECT * FROM Appeals a WHERE a.client_id = ?1 ORDER BY a.created_at DESC limit 1;")
    Optional<Appeals> findTopByClientIdOrderByCreatedAtDesc(String clientId);

}