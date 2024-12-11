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
public class BalanceProcessor {
    private final CallBackDataProcessor callBackDataProcessor;
    private final UtilService utilService;
    private final FeignService feignService;
    public void processor(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        if (update.hasMessage()){
            feignService.sendBalance(chatId);
        } else if (update.hasCallbackQuery()) {
            callBackDataProcessor.processor(update);
        }
    }
}

