package uz.dataFin.notificationbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.dataFin.notificationbot.model.DateDTO;

import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<DateDTO, Long> {
    Optional<DateDTO> findDateDTOByClientId(String clientId);
    DateDTO getDateDTOByClientId(String clientId);

}
