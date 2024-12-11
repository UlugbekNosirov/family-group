package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.service.processor.*;
import uz.dataFin.notificationbot.utils.BotState;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WebhookService {
    private final BotService botService;
    private final UserService userService;
    private final UtilService utilService;
    private final ReportService reportService;
    private final AKTSVERKAProcessor aktsverkaProcessor;
    private final CallBackDataProcessor callBackDataProcessor;
    private final RegistrationProcessor registrationProcessor;
    private final SettingsProcessor settingsProcessor;
    private final BalanceProcessor balanceProcessor;
    private final AdvertisingProcessor advertisingProcessor;
    private final VideoProcessor videoProcessor;
    private final PhotoProcessor photoProcessor;

    public void onUpdateToReceive(Update update) {
        BotState state = botService.getAndCheck(update);
        String chatId = utilService.getChatIdFromUpdate(update);
        if (state==BotState.UPDATE_IS_NULL){ utilService.sendMessage(chatId, "\uD83D\uDEABJarayonni qaytadan boshlang\uD83D\uDEAB", null, null); return;}
        Message message = utilService.getMessageFromUpdate(update);
        String callBackData = (update.hasCallbackQuery()) ? update.getCallbackQuery().getData() : message.getText();

        if (Objects.nonNull(callBackData) || message.hasContact()) {
            userService.saveRole(chatId, userService.getRoleInURL(chatId));
            reportService.setReportType(chatId, callBackData);

            try {
                if (!userService.isRegistered(chatId) || callBackData.equals("/start")) {
                    registrationProcessor.processor(update);
                    return;
                }
            }catch (Exception e){
                System.out.println(e+"\n\ncall back data is null\n\n");
            }
        }

        String typeReport = reportService.getReportDto(chatId).getTypeReport();

        if (update.hasMessage()) {
            if (message.hasText()) {
                switch (typeReport){
                    case "AKT_SVERKA", "AKT_SVERKA_TOVAR" -> aktsverkaProcessor.processor(update);
                    case "BALANCE" -> balanceProcessor.processor(update);
                    case "ADS" -> advertisingProcessor.processor(update);
                    case "SETTINGS" -> settingsProcessor.processor(update);
                }
            } else if (message.hasPhoto()) {
                photoProcessor.processor(update);
            } else if (message.hasVideo()) {
                videoProcessor.processor(update);
            }
        } else if (update.hasCallbackQuery()) {
            callBackDataProcessor.processor(update);
        }
    }
}