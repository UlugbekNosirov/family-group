package uz.dataFin.notificationbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.dataFin.notificationbot.model.Users;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Users,Long> {
    Optional<Users> findByChatId(String chatId);
    Optional<Users> findRoleByChatId(String chatId);

}