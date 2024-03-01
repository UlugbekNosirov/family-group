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

@Service
@RequiredArgsConstructor
public class WarehouseProcessor {
    private final CallBackDataProcessor callBackDataProcessor;
    private final UtilService utilService;
    private final WarehouseService warehouseService;
    private final FeignService feignService;
    private final Api1CService api1CService;
    private final UserService userService;
    private final ReportService reportService;
    private final SearchRepository searchRepository;
    public static final String[] ReportAKTSteps = {"START", "GET_WAREHOUSE", "GET_PRODUCT", "GET_PRODUCT_IN_API", "GET_PRODUCT_GROUP_IN_API", "GET_REPORT_WAREHOUSE"};
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
            } else if (warehouseService.getByClick(text) || text.equals(utilService.getTextByLanguage(chatId, Constant.CHOOSE_PRODUCT))) {
                STATE_NAME = BotState.GET_PRODUCT.name();
                userService.updateUserState(chatId, BotState.GET_PRODUCT);
                reportService.saveWareHouseId(chatId, warehouseService.getByName(text));
            } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.ALL_PRODUCT_GROUP)) || text.equals(utilService.getTextByLanguage(chatId, Constant.NO_FILTR_PRODUCT))) {
                STATE_NAME = BotState.GET_REPORT_WAREHOUSE.name();
                reportService.saveProductIdNONE(chatId);
            } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.PRODUCT))) {
                STATE_NAME = BotState.GET_PRODUCT_IN_API.name();
                userService.updateUserState(chatId, BotState.GET_PRODUCT_IN_API);
            } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.PRODUCT_GROUP))) {
                STATE_NAME = BotState.GET_PRODUCT_GROUP_IN_API.name();
                userService.updateUserState(chatId, BotState.GET_PRODUCT_GROUP_IN_API);
            } else if (api1CService.haveProductGroup(state, new SearchDTO(chatId, text, 1, 200))) {
                STATE_NAME = BotState.GET_REPORT_WAREHOUSE.name();
                reportService.saveProductId(state, chatId, text);
            }else if (state == BotState.GET_PRODUCT_IN_API || state == BotState.GET_PRODUCT_GROUP_IN_API){
                STATE_NAME = BotState.GET_PRODUCTS_IN_API.name();
            }
            switch (STATE_NAME){
                case "DEFAULT" -> feignService.sendPanelBTNS(chatId);
                case "START" -> feignService.sendDate(chatId);
                case "GET_WAREHOUSE" -> feignService.sendWarehouse(chatId);
                case "GET_PRODUCT" -> feignService.sendProductBtns(chatId);
                case "PAGINATION" -> feignService.pagination(state, chatId, message);
                case "GET_PRODUCT_IN_API", "GET_PRODUCT_GROUP_IN_API" -> feignService.sendProductInAPI(chatId, userService.getState(chatId));
                case "GET_REPORT_WAREHOUSE" -> feignService.sendReport(chatId);
                case "GET_PRODUCTS_IN_API" -> feignService.sendProducts(state, chatId, text);
            }
        } else if (update.hasCallbackQuery()) {
            callBackDataProcessor.processor(update);
        }
    }
}
