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
public class CashBoxProcessor {
    private final CallBackDataProcessor callBackDataProcessor;
    private final UtilService utilService;
    private final WarehouseService warehouseService;
    private final FeignService feignService;
    private final Api1CService api1CService;
    private final UserService userService;
    private final ReportService reportService;

    public static final String[] ReportAKTSteps = {"START", "GET_CASH_BOX", "GET_REPORT_CASH_BOX"};
    public void processor(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        if (reportService.getReportDto(chatId).getTypeReport().equals("CASH_BOX_SMENA")){feignService.sendReport(chatId);return;}

        BotState state = userService.getState(chatId);
        Message message = utilService.getMessageFromUpdate(update);
        String STATE_NAME = state.name();
        if (update.hasMessage()){
            String text = message.getText();
            if (text.equals(utilService.getTextByLanguage(chatId, Constant.BACK_STATE))){
                STATE_NAME = utilService.getPreviousStep(state, ReportAKTSteps);
            } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.BACK))) {
                STATE_NAME = BotState.DEFAULT.name();
            } else if (warehouseService.getByClick(text)) {
                STATE_NAME = BotState.GET_REPORT_CASH_BOX.name();
                userService.updateUserState(chatId, BotState.GET_REPORT_CASH_BOX);
                reportService.saveCashID(chatId, text);
            }
            switch (STATE_NAME){
                case "DEFAULT" -> feignService.sendPanelBTNS(chatId);
                case "START" -> feignService.sendDate(chatId);
                case "GET_CASH_BOX" -> feignService.sendCashBox(chatId);
                case "GET_REPORT_CASH_BOX" -> feignService.sendReport(chatId);
            }
        } else if (update.hasCallbackQuery()) {
            callBackDataProcessor.processor(update);
        }
    }
}
