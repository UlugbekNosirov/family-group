package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.helper.Keyboard;
import uz.dataFin.notificationbot.service.processor.*;
import uz.dataFin.notificationbot.utils.BotState;
import uz.dataFin.notificationbot.utils.Constant;

@Service
@RequiredArgsConstructor
public class WebhookService {
    private final BotService botService;
    private final UserService userService;
    private final UtilService utilService;
    private final WarehouseProcessor warehouseProcessor;
    private final Keyboard keyboard;
    private final ReportService reportService;
    private final AKTSVERKAProcessor aktsverkaProcessor;
    private final CostProcessor costProcessor;
    private final TradeProcessor tradeProcessor;
    private final CashBoxProcessor cashBoxService;
    private final DebitorCreditorProcessor creditorProcessor;
    private final CallBackDataProcessor callBackDataProcessor;
    private final RegistrationProcessor registrationProcessor;
    private final SettingsProcessor settingsProcessor;

    public void onUpdateToReceive(Update update) {
        BotState state = botService.getAndCheck(update);
        String chatId = utilService.getChatIdFromUpdate(update);
        Message message = utilService.getMessageFromUpdate(update);
        String callBackData = (update.hasCallbackQuery()) ? update.getCallbackQuery().getData() : message.getText();

        userService.saveRole(chatId, userService.getRoleInURL(chatId));
        reportService.setReportType(chatId, callBackData);

        if (!userService.isRegistered(chatId) || callBackData.equals("/start")) {registrationProcessor.processor(update);return;}

        String typeReport = reportService.getReportDto(chatId).getTypeReport();
        if (update.hasMessage()) {
            if (message.hasText()) {
                switch (typeReport){
                    case "AKT_SVERKA", "AKT_SVERKA_TOVAR" -> aktsverkaProcessor.processor(update);
                    case "WAREHOUSE" -> warehouseProcessor.processor(update);
                    case "TRADE", "TRADE_ALKAN" -> tradeProcessor.processor(update);
                    case "CASH_BOX" -> cashBoxService.processor(update);
                    case "COST" -> costProcessor.processor(update);
                    case "DC" -> creditorProcessor.processor(update);
                    case "SETTINGS" -> settingsProcessor.processor(update);
                }
                if (state == BotState.DEFAULT){
                    utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.MAIN_MENU), keyboard.panelBtns(userService.getRole(chatId), chatId), null);
                }
            }
        } else if (update.hasCallbackQuery()) {
            callBackDataProcessor.processor(update);
        }
    }
}