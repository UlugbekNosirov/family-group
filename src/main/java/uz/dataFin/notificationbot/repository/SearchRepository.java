package uz.dataFin.notificationbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.dataFin.notificationbot.model.SearchDTO;
import uz.dataFin.notificationbot.model.WarehouseDTO;

import java.util.Optional;

@Repository
public interface SearchRepository extends JpaRepository<SearchDTO, Long> {
    Optional<SearchDTO> findByChatId(String chatId);

}
