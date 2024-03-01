package uz.dataFin.notificationbot.service.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.model.SearchDTO;
import uz.dataFin.notificationbot.service.*;
import uz.dataFin.notificationbot.utils.BotState;
import uz.dataFin.notificationbot.utils.Constant;

@Service
@RequiredArgsConstructor
public class DebitorCreditorProcessor {
    private final CallBackDataProcessor callBackDataProcessor;
    private final UtilService utilService;
    private final WarehouseService warehouseService;
    private final FeignService feignService;
    private final UserService userService;
    private final ReportService reportService;

    public static final String[] ReportAKTSteps = {"START", "GET_BRANCH", "GET_TYPE_CONTRACTOR", "GET_REPORT_DC"};

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
            } else if (warehouseService.getByClickBranch(text)) {
                STATE_NAME = BotState.GET_TYPE_CONTRACTOR.name();
                userService.updateUserState(chatId, BotState.GET_TYPE_CONTRACTOR);
                reportService.saveBranchID(chatId, warehouseService.getByNameIsBranch(text));
            } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.CLIENT_FOR_KEYBOARD)) || text.equals(utilService.getTextByLanguage(chatId, Constant.POSTAVSHIK)) || text.equals(utilService.getTextByLanguage(chatId, Constant.ALL))) {
                STATE_NAME = BotState.GET_REPORT_DC.name();
                reportService.saveTypeContractor(chatId, getNumberOfTypeContractor(chatId, text));
            }
            switch (STATE_NAME){
                case "DEFAULT" -> feignService.sendPanelBTNS(chatId);
                case "START" -> feignService.sendDate(chatId);
                case "GET_BRANCH" -> feignService.sendBranches(chatId);
                case "GET_TYPE_CONTRACTOR" -> feignService.sendTypeContractorsBtns(chatId);
                case "GET_REPORT_DC" -> feignService.sendReport(chatId);
            }
        } else if (update.hasCallbackQuery()) {
            callBackDataProcessor.processor(update);
        }
    }

    private int getNumberOfTypeContractor(String chatId, String text) {
        if (text.equals(utilService.getTextByLanguage(chatId, Constant.CLIENT_FOR_KEYBOARD)))
            return 2;
        else if (text.equals(utilService.getTextByLanguage(chatId, Constant.POSTAVSHIK)))
            return 1;
        return 3;
    }
}
