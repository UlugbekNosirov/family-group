package uz.dataFin.notificationbot.service.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.model.SearchDTO;
import uz.dataFin.notificationbot.repository.SearchRepository;
import uz.dataFin.notificationbot.service.*;
import uz.dataFin.notificationbot.utils.BotState;
import uz.dataFin.notificationbot.utils.Constant;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class AKTSVERKAProcessor{
    private final CallBackDataProcessor callBackDataProcessor;
    private final UtilService utilService;
    private final WarehouseService warehouseService;
    private final FeignService feignService;
    private final Api1CService api1CService;
    private final UserService userService;
    private final ReportService reportService;
    public static final String[] ReportAKTSteps = {"START", "GET_BRANCH", "GET_CONTRACTORS", "GET_REPORT_SVERKA"};
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
            } else if (text.equals("-->>") || text.equals("<<--")) {
                STATE_NAME = BotState.PAGINATION.name();
            } else if (warehouseService.getByClickBranch(text)) {
                STATE_NAME = BotState.GET_CONTRACTORS.name(); //branchId ni save qilish kerak
                userService.updateUserState(chatId, BotState.GET_CONTRACTORS);
                reportService.saveBranchID(chatId, warehouseService.getByNameIsBranch(text));
                reportService.saveSearch(chatId, reportService.getReportDto(chatId).getBranchID(), "");
            } else if (state == BotState.GET_CONTRACTORS && api1CService.haveProductGroup(BotState.GET_CONTRACTORS, new SearchDTO(chatId, reportService.getReportDto(chatId).getBranchID(), text, 1, 200))) {
                STATE_NAME = BotState.GET_REPORT_SVERKA.name();
                reportService.saveClientId(chatId, text);
            } else if (state == BotState.GET_CONTRACTORS && text.equals(utilService.getTextByLanguage(chatId, Constant.NO_FILTR_PRODUCT))) {
                STATE_NAME = BotState.GET_REPORT_SVERKA.name();
                reportService.saveClientId(chatId, text);
            }
            switch (STATE_NAME){
                case "DEFAULT" -> feignService.sendPanelBTNS(chatId);
                case "START" -> feignService.sendDate(chatId);
                case "GET_BRANCH" -> feignService.sendBranches(chatId);
                case "GET_CONTRACTORS" -> feignService.sendContractors(chatId, text);
                case "PAGINATION" -> feignService.pagination(state, chatId, message);
                case "GET_REPORT_SVERKA" -> feignService.sendReport(chatId);
            }
        } else if (update.hasCallbackQuery()) {
            callBackDataProcessor.processor(update);
        }
    }
}
