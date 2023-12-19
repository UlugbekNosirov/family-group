package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.dataFin.notificationbot.model.MessageDTO;
import uz.dataFin.notificationbot.model.UpdateUserStatus;
import uz.dataFin.notificationbot.model.Users;
import uz.dataFin.notificationbot.payload.UserDTO;
import uz.dataFin.notificationbot.repository.MarketRepository;
import uz.dataFin.notificationbot.repository.UpdateUserStatusRepository;
import uz.dataFin.notificationbot.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UpdateUserStatusService {
    private final UpdateUserStatusRepository updateUserStatusRepository;

    public void saveData(UpdateUserStatus updateUserStatus) {
        Optional<UpdateUserStatus> optionalUser = updateUserStatusRepository.findByDocumentID(updateUserStatus.getDocumentID());
        if (optionalUser.isPresent()){
            UpdateUserStatus updateUserStatus1 = optionalUser.get();
            updateUserStatus1.setChatId(updateUserStatus.getChatId());
            updateUserStatus1.setText(updateUserStatus.getText());
            updateUserStatus1.setClientId(updateUserStatus.getClientId());
            updateUserStatus1.setTypeDocument(updateUserStatus.getTypeDocument());
            updateUserStatus1.setStatus("rejected");
            updateUserStatusRepository.save(updateUserStatus1);
        }else{
            updateUserStatusRepository.save(updateUserStatus);
        }
    }

    public UpdateUserStatus getData(String chatId, String text ) {
        Optional<UpdateUserStatus> optionalUser = updateUserStatusRepository.getByDocumentIDAndChatId(chatId, text);
        return optionalUser.orElseGet(UpdateUserStatus::new);
    }

    public UpdateUserStatus updateStatus(UpdateUserStatus data, String status) {
        Optional<UpdateUserStatus> optionalUser = updateUserStatusRepository.findByDocumentID(data.getDocumentID());
        if (optionalUser.isPresent()){
            UpdateUserStatus updateUserStatus = optionalUser.get();
            updateUserStatus.setStatus(status);
            updateUserStatusRepository.save(updateUserStatus);
            return updateUserStatus;
        }
        return updateUserStatusRepository.save(data);
    }
}
