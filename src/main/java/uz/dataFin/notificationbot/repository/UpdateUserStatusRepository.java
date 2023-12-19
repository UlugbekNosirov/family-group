package uz.dataFin.notificationbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uz.dataFin.notificationbot.model.UpdateUserStatus;
import uz.dataFin.notificationbot.model.Users;

import java.util.Optional;

public interface UpdateUserStatusRepository extends JpaRepository<UpdateUserStatus,Long> {

    @Query(nativeQuery = true, value = "select * from update_user_status i where  i.documentid = ?1")
    Optional<UpdateUserStatus> findByDocumentID(String documentId);

    @Query(nativeQuery = true, value = "select * from update_user_status i where  i.text = ?2 and i.chat_id = ?1")
    Optional<UpdateUserStatus> getByDocumentIDAndChatId(String chatId,  String text);
}