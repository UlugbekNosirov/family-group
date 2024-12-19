package uz.dataFin.notificationbot.service.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.service.FeignService;
import uz.dataFin.notificationbot.service.GroupService;
import uz.dataFin.notificationbot.service.UserService;
import uz.dataFin.notificationbot.service.UtilService;
import uz.dataFin.notificationbot.utils.BotState;

@Service
@RequiredArgsConstructor
public class AppealProcessor {
    private final CallBackDataProcessor callBackDataProcessor;
    private final UtilService utilService;
    private final FeignService feignService;
    private final UserService userService;
    private final GroupService groupService;
//    public static final String[] ReportADSSteps = {"START", "GET", "GET_LANGUAGE"};

    public void processor(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        BotState state = userService.getState(chatId);
        Message message = utilService.getMessageFromUpdate(update);
        String callBackData = (update.hasCallbackQuery()) ? update.getCallbackQuery().getData() : message.getText();

        String STATE_NAME = state.name();
//        if (!userService.getRoleInURL(chatId).equals("Contractor")) {
//            return;
//        }
        if (update.hasMessage()){
            switch (STATE_NAME){
                case "DEFAULT" -> feignService.sendPanelBTNS(chatId);
                case "START" -> feignService.sendAppealText(chatId);
                case "GET_APPEAL_TEXT" -> feignService.checkAppealText(chatId, callBackData);
                case "CHECK_APPEAL" -> feignService.checkAppealText(chatId, callBackData);
            }
        } else if (update.hasCallbackQuery()) {
            if (state == BotState.CHECK_APPEAL){
                if (callBackData.equals("YES"))
                    feignService.sendAppealGroup(groupService.getAllGroups(), update);
                else if (callBackData.equals("NOYES")) {
                    feignService.rejectAppeal(chatId, message);
                }
            }
        }
    }
}
