package uz.dataFin.notificationbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.dataFin.notificationbot.model.Users;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users,Long> {
    @Query(nativeQuery = true, value = "select * from users i where i.chat_id =?1")
    Optional<Users> findByChatId(String chatId);
    @Query(nativeQuery = true, value = "select * from users i where i.chat_id =?1")
    Optional<Users> findRoleByChatId(String chatId);

    @Query(nativeQuery = true, value = "select * from users")
    List<Users> findAllByActive();
}