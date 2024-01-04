package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.model.SearchDTO;
import uz.dataFin.notificationbot.payload.UserDTO;
import uz.dataFin.notificationbot.repository.SearchRepository;
import uz.dataFin.notificationbot.utils.BotState;
import uz.dataFin.notificationbot.utils.Constant;

@Service
@RequiredArgsConstructor
public class WebhookService {
    private final BotService botService;
    private final UserService userService;
    private final UtilService utilService;
    private final WarehouseService warehouseService;
    private final Api1CService api1CService;
    private final ReportService reportService;
    private final SearchRepository searchRepository;

    public void onUpdateToReceive(Update update) {
        BotState state = botService.getAndCheck(update);
        String chatId = utilService.getChatIdFromUpdate(update);
        Integer messageId = utilService.getMessageIdFromUpdate(update);
        Message message = utilService.getMessageFromUpdate(update);
        String callBackData = (update.hasCallbackQuery()) ? update.getCallbackQuery().getData() : message.getText();
        System.out.println(state);
        if (update.hasMessage()) {
            if (message.hasText()) {
                String text = message.getText();
                if (text.equals("/start")) {
                    state = BotState.SEND_PHONE;
                } else if (utilService.containsSpecialCharacters(message.getFrom().getFirstName()) && state == BotState.SEND_PHONE) {
                    state = BotState.SAVE_NAME;
                } else if (text.equals("Tovar tanlash")) {
                    state = BotState.SAVE_WAREHOUSE;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.BACK_STATE)) && state == BotState.SAVE_WAREHOUSE) {
                    state = BotState.SEND_WAREHOUSE_BACK;
                } else if (reportService.getReportDto(chatId).getTypeReport().startsWith("\uD83D\uDCC5AKT") && state == BotState.SAVE_WAREHOUSE) {
                    state = BotState.GET_CONTRACTOR_IN_API;
                } else if (state == BotState.GET_CONTRACTOR_IN_API && text.equals(utilService.getTextByLanguage(chatId, Constant.BACK_STATE))) {
                    state = BotState.GET_CONTRACTOR_IN_API;
                } else if (state == BotState.GET_CONTRACTOR_IN_API && api1CService.haveProductGroup(state, new SearchDTO(chatId, searchRepository.findByChatId(chatId).get().getBranchID(), text, 1, 200))) {
                    state = BotState.GET_REPORT_SVERKA;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.BACK_STATE)) && state == BotState.GET_BY_PRODUCT_GROUP) {
                    state = BotState.SAVE_WAREHOUSE;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.BACK_STATE)) && state == BotState.GET_BY_PRODUCT) {
                    state = BotState.SAVE_WAREHOUSE;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.SETTINGS))) {
                    state = BotState.SEND_BTN_SETTING;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.CHANGE_TYPE_FILE))) {
                    state = BotState.SEND_BTN_TYPE_FILE;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.CHANGE_LANGUAGE))) {
                    state = BotState.SEND_BTN_CHANGE_LANG;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.BACK))) {
                    state = BotState.SEND_BTN_MAIN_MANU;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.BALANCE))) {
                    state = BotState.GET_BALANCE;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.WAREHOUSE))) {
                    state = BotState.GET_START_DATE_WAREHOUSE;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.TRADE))) {
                    state = BotState.GET_START_DATE_TRADE;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.CASH_BOX))) {
                    state = BotState.GET_START_DATE_CASH_BOX;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.PRODUCT))) {
                    state = BotState.GET_BY_PRODUCT;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.ACCOUNT_DEBT))) {
                    state = BotState.GET_PERIOD;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.AKT_SVERKA_TOVAR))) {
                    state = BotState.GET_START_DATE;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.AKT_SVERKA))) {
                    state = BotState.GET_START_DATEV2;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.COST))) {
                    state = BotState.GET_START_DATE_COST;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.NO_FILTR_PRODUCT))) {
                    state = BotState.NO_FILTR_PRODUCT;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.ALL_PRODUCT_GROUP))) {
                    state = BotState.NO_FILTR_PRODUCT_GROUP;
                } else if (state == BotState.GET_BY_PRODUCT) {
                    state = BotState.GET_PRODUCT_IN_API;
                } else if (state == BotState.GET_PRODUCT_IN_API && message.getText().equals(utilService.getTextByLanguage(chatId, Constant.BACK_STATE))) {
                    state = BotState.SAVE_WAREHOUSE;
                } else if (state == BotState.GET_PRODUCT_IN_API && api1CService.haveProductGroup(state, new SearchDTO(chatId, message.getText(), 1, 200))) {
                    state = BotState.GET_REPORT_WAREHOUSE_BY_PRODUCT;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.PRODUCT_GROUP))) {
                    state = BotState.GET_BY_PRODUCT_GROUP;
                } else if (state == BotState.GET_BY_PRODUCT_GROUP) {
                    state = BotState.GET_PRODUCT_GROUP_IN_API;
                } else if (text.equals("-->>") || text.equals("<<--")) {
                    state = BotState.NEXT;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.CLIENT)) || text.equals(utilService.getTextByLanguage(chatId, Constant.POSTAVSHIK)) || text.equals(utilService.getTextByLanguage(chatId, Constant.ALL))) {
                    state = BotState.GET_ACCOUNT_DEBT;
                } else if (state == BotState.GET_PRODUCT_GROUP_IN_API && message.getText().equals(utilService.getTextByLanguage(chatId, Constant.BACK_STATE))) {
                    state = BotState.SAVE_WAREHOUSE;
                } else if (state == BotState.GET_PRODUCT_GROUP_IN_API && api1CService.haveProductGroup(state, new SearchDTO(chatId, message.getText(), 1, 200))) {
                    state = BotState.GET_REPORT_WAREHOUSE;
                } else if (!reportService.getReportDto(chatId).getTypeReport().equals("DC") && warehouseService.getByClick(text)) {
                    state = BotState.SAVE_WAREHOUSE;
                } else if (reportService.getReportDto(chatId).getTypeReport().equals("DC") && warehouseService.getByClickBranch(text)) {
                    state = BotState.SAVE_WAREHOUSE;
                } else if (reportService.getReportDto(chatId).getTypeReport().startsWith("\uD83D\uDCC5AKT") && warehouseService.getByClickBranch(text)) {
                    state = BotState.SAVE_WAREHOUSE;
                } else if (reportService.getReportDto(chatId).getTypeReport().equals("COST") && warehouseService.getByClickBranch(text)) {
                    state = BotState.SAVE_WAREHOUSE;
                } else if (UtilService.containsOnlyNumbers(text)) {
                    state = BotState.GET_PRODUCT;
                } else if (state.toString().startsWith("EDIT2")) {
                    state = BotState.SEND_CONTINUE;
                }
            } else if (message.hasContact()) {
                state = BotState.GET_CONTACT;
                userService.updateUserState(update, state, message.getContact().getPhoneNumber());
            }
        }

        if (update.hasCallbackQuery()) {
            if (utilService.getErrorDate(update)) {
                state = BotState.SEND_EXCEPTION;
            } else {
                switch (callBackData) {
                    case "agreed" -> state = BotState.SEND_AGREED_MONEY;
                    case "rejected" -> state = BotState.SEND_REJECTED_MONEY;
                    case "pdf" -> state = BotState.EDIT2PDF;
                    case "jpg" -> state = BotState.EDIT2JPG;
                    case "xlsx" -> state = BotState.EDIT2XLSX;
                    case "uz" -> state = BotState.EDIT2UZ;
                    case "ru" -> state = BotState.EDIT2RU;
                    case "kril" -> state = BotState.EDIT2KRIL;
                    case "day" -> state = BotState.SEND_BY_DAY;
                    case "week" -> state = BotState.SEND_BY_WEEK;
                    case "month" -> state = BotState.SEND_BY_LAST_MONTH;
                    case "season" -> state = BotState.SEND_BY_SEASON;
                    case "year" -> state = BotState.SEND_BY_YEAR;
                    case "other" -> state = BotState.SEND_CALENDAR;
                }
                ;
                switch (state) {
                    case GET_START_DATE, GET_START_DATEV2, GET_START_DATE_WAREHOUSE, GET_PERIOD, GET_START_DATE_COST -> {
                        if (!utilService.checkBADate(callBackData) && !utilService.getMonth(update)) {
                            state = BotState.SAVE_START_DATE;
                        } else if (utilService.checkBADate(callBackData)) {
                            state = BotState.EDIT_START_DATE;
                        } else if (utilService.getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        }
                    }
                    case EDIT_START_DATE -> {
                        if (!utilService.checkBADate(callBackData) && !utilService.getMonth(update)) {
                            state = BotState.SAVE_START_DATE;
                        } else if (utilService.getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        }
                    }
                    case SAVE_START_DATE -> {
                        if (!utilService.checkBADate(callBackData) && !utilService.getMonth(update)) {
                            state = BotState.REPORTS;
                        } else if (utilService.checkBADate(callBackData)) {
                            state = BotState.EDIT_END_DATE;
                        } else if (utilService.getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        }
                    }
                    case EDIT_END_DATE -> {
                        if (!utilService.checkBADate(callBackData) && utilService.getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        } else if (!utilService.checkBADate(callBackData) && !utilService.getMonth(update)) {
                            state = BotState.REPORTS;
                        } else if (utilService.getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        }
                    }
                }
                if (callBackData.equals(Constant.ENTER_MARKET_NAME_BTN)) {
                    state = BotState.SEND_MARKET_NAME_TEXT;
                }
            }
        }


        switch (state) {
            case SEND_PHONE, GET_CONTACT -> botService.getMainMenuSend(chatId);
            case GET_PRODUCT -> botService.Employee(message);
            case SEND_AGREED_MONEY, SEND_REJECTED_MONEY -> botService.sendCheckingMoney(state, message);
            case SAVE_NAME -> botService.saveName(message, chatId);
            case GET_START_DATE, GET_START_DATEV2, GET_START_DATE_WAREHOUSE, GET_START_DATE_TRADE,
                    GET_START_DATE_CASH_BOX, GET_PERIOD, GET_START_DATE_COST -> botService.sendStartDate(state, chatId);
            case SEND_CALENDAR -> botService.sendStartDateAsCalendar(message, chatId);
            case REPORTS, SEND_BY_MONTH, SEND_BY_DAY, SEND_BY_WEEK, SEND_BY_LAST_MONTH, SEND_BY_SEASON, SEND_BY_YEAR ->
                    botService.getReport(state, chatId, messageId, callBackData);
            case SAVE_START_DATE -> botService.saveStartDate(message, chatId, callBackData);
            case SEND_WAREHOUSE_BACK -> botService.sendWareHouseButton(state, chatId, messageId);
            case SAVE_WAREHOUSE -> botService.saveWarehouse(chatId, message);
            case SEND_EXCEPTION -> botService.sendError(chatId, messageId);
            case GET_BY_PRODUCT_GROUP, GET_BY_PRODUCT -> botService.getByProductGroup(state, chatId);
            case GET_REPORT_WAREHOUSE, NO_FILTR_PRODUCT_GROUP, NO_FILTR_PRODUCT,
                    GET_REPORT_WAREHOUSE_BY_PRODUCT -> botService.getReportWarehouse(state, chatId, message);
            case GET_PRODUCT_GROUP_IN_API, GET_PRODUCT_IN_API, GET_CONTRACTOR_IN_API ->
                    botService.getByProductGroupByAPI(state, chatId, message);
            case GET_REPORT_SVERKA -> botService.getReportSVERKA(chatId, message);
            case GET_ACCOUNT_DEBT -> botService.getAccountDebt(chatId, message);
            case GET_BALANCE -> botService.getBalance(chatId);
            case EDIT_START_DATE, EDIT_END_DATE -> botService.editDate(chatId, messageId, callBackData);
            case SEND_BTN_SETTING -> botService.sendSettings(chatId);
            case SEND_BTN_TYPE_FILE -> botService.sendTypeFile(chatId);
            case NEXT -> botService.next(chatId, message);
            case SEND_BTN_CHANGE_LANG -> botService.sendLanguage(chatId);
            case SEND_BTN_MAIN_MANU -> botService.sendMainMenu(chatId);
            case SEND_CONTINUE -> botService.sendContinue(chatId);
            case EDIT2XLSX, EDIT2JPG, EDIT2PDF -> botService.editTypeFile(state, chatId, messageId, callBackData);
            case EDIT2UZ, EDIT2RU, EDIT2KRIL -> botService.editLanguage(state, chatId, messageId, callBackData);
        }
        if (state != BotState.SAVE_NAME)
            if (state != BotState.SEND_CALENDAR)
                if (state != BotState.SEND_AGREED_MONEY)
                    if (state != BotState.GET_REPORT_WAREHOUSE)
                        if (state != BotState.NEXT)
                            botService.saveData(update, new UserDTO(state));
    }
}