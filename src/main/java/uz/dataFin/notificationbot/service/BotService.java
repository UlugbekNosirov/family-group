package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import uz.dataFin.notificationbot.model.dtos.UserDTO;
import uz.dataFin.notificationbot.utils.BotState;
@Service
@RequiredArgsConstructor
public class BotService {
    private final UserService userService;

    public BotState getAndCheck(Update update) {
        UserDTO userDTO = userService.checkAndGet(update);
        return userDTO.getState();
    }

}