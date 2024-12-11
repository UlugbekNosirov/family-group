package uz.dataFin.notificationbot.service.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.service.FeignService;
import uz.dataFin.notificationbot.service.UserService;
import uz.dataFin.notificationbot.service.UtilService;
import uz.dataFin.notificationbot.utils.BotState;
import uz.dataFin.notificationbot.utils.Constant;

@Service
@RequiredArgsConstructor
public class AdvertisingProcessor {
    private final CallBackDataProcessor callBackDataProcessor;
    private final UtilService utilService;
    private final FeignService feignService;
    private final UserService userService;
    public static final String[] ReportADSSteps = {"START", "GET_TYPE_FILE", "GET_LANGUAGE"};

    public void processor(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        BotState state = userService.getState(chatId);
        Message message = utilService.getMessageFromUpdate(update);
        String STATE_NAME = state.name();
        if (!userService.getRoleInURL(chatId).equals("Employee")) {
            feignService.sendPanelBTNS(chatId);
            return;
        }
        if (update.hasMessage()){
            switch (STATE_NAME){
                case "DEFAULT" -> feignService.sendPanelBTNS(chatId);
                case "START" -> feignService.sendAds(chatId);
            }
        } else if (update.hasCallbackQuery()) {
            callBackDataProcessor.processor(update);
        }
    }
}
