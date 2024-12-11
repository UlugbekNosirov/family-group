package uz.dataFin.notificationbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.dataFin.notificationbot.model.Advertising;

import java.util.Optional;

public interface AdvertisingRepository extends JpaRepository<Advertising,Long> {
    Optional<Advertising> findByClientID(String clientID);

}