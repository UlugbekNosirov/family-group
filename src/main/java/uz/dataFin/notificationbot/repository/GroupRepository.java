package uz.dataFin.notificationbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.dataFin.notificationbot.model.Advertising;
import uz.dataFin.notificationbot.model.Groups;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Groups,Long> {
    Optional<Groups> findByGroupId(String groupID);

}