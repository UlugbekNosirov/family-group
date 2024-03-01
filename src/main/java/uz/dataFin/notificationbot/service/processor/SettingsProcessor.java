package uz.dataFin.notificationbot.service.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.service.*;
import uz.dataFin.notificationbot.utils.BotState;
import uz.dataFin.notificationbot.utils.Constant;

@Service
@RequiredArgsConstructor
public class SettingsProcessor {
    private final CallBackDataProcessor callBackDataProcessor;
    private final UtilService utilService;
    private final WarehouseService warehouseService;
    private final FeignService feignService;
    private final UserService userService;
    private final ReportService reportService;
    public static final String[] ReportAKTSteps = {"START", "GET_TYPE_FILE", "GET_LANGUAGE"};

    public void processor(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        BotState state = userService.getState(chatId);
        Message message = utilService.getMessageFromUpdate(update);
        String STATE_NAME = state.name();
        if (update.hasMessage()){
            String text = message.getText();
            if (text.equals(utilService.getTextByLanguage(chatId, Constant.BACK_STATE))){
                STATE_NAME = utilService.getPreviousStep(state, ReportAKTSteps);
            } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.BACK))) {
                STATE_NAME = BotState.DEFAULT.name();
            } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.CHANGE_TYPE_FILE))) {
                STATE_NAME = BotState.GET_TYPE_FILE.name();
                userService.updateUserState(chatId, BotState.GET_REPORT_COST);
            } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.CHANGE_LANGUAGE))) {
                STATE_NAME = BotState.GET_LANGUAGE.name(); //branchId ni save qilish kerak
                userService.updateUserState(chatId, BotState.GET_LANGUAGE);
            }
            switch (STATE_NAME){
                case "DEFAULT" -> feignService.sendPanelBTNS(chatId);
                case "START" -> feignService.sendSettings(chatId);
                case "GET_TYPE_FILE" -> feignService.sendTypeFile(chatId);
                case "GET_LANGUAGE" -> feignService.sendLanguage(chatId);
            }
        } else if (update.hasCallbackQuery()) {
            callBackDataProcessor.processor(update);
        }
    }
}
