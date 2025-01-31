package uz.dataFin.notificationbot.service.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.service.*;
import uz.dataFin.notificationbot.utils.BotState;


@Service
@RequiredArgsConstructor
public class AKTSVERKAProcessor{
    private final CallBackDataProcessor callBackDataProcessor;
    private final UtilService utilService;
    private final FeignService feignService;
    private final UserService userService;
    public void processor(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        BotState state = userService.getState(chatId);

        String STATE_NAME = state.name();

        if (update.hasMessage()){
            switch (STATE_NAME){
                case "DEFAULT" -> feignService.sendPanelBTNS(chatId);
                case "START" -> feignService.sendDate(chatId);
            }
        } else if (update.hasCallbackQuery()) {
            callBackDataProcessor.processor(update);
        }
    }
}
