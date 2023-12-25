package uz.dataFin.notificationbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.dataFin.notificationbot.model.DateDTO;
import uz.dataFin.notificationbot.model.ReportDTO;

import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<ReportDTO, Long> {
    Optional<ReportDTO> findByClientId(String clientId);


}
