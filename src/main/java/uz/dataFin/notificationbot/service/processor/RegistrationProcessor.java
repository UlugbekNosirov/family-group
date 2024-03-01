package uz.dataFin.notificationbot.service.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.service.BotService;
import uz.dataFin.notificationbot.service.FeignService;
import uz.dataFin.notificationbot.service.UserService;
import uz.dataFin.notificationbot.service.UtilService;
import uz.dataFin.notificationbot.utils.BotState;

@Service
@RequiredArgsConstructor
public class RegistrationProcessor {
    private final UtilService utilService;
    private final FeignService feignService;
    private final UserService userService;

    public void processor(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        BotState state = userService.getState(chatId);
        Message message = utilService.getMessageFromUpdate(update);

        if (update.hasMessage()) {
            if (message.hasText()) {
                state = userService.startCondition(update);
            } else if (message.hasContact()) {
                state = BotState.SAVE_CONTACT;
            }
        }
        switch (state){
            case SEND_NAME -> feignService.sendName(chatId);
            case GET_CONTACT -> feignService.sendContact(chatId);
            case SAVE_CONTACT, SUCCESS -> feignService.saveContact(message, chatId);
        }

        userService.updateUserState(chatId, state);
    }
}
