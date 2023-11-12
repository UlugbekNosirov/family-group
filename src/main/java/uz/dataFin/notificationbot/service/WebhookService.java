package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.payload.UserDTO;
import uz.dataFin.notificationbot.utils.BotState;
import uz.dataFin.notificationbot.utils.Constant;

@Service
@RequiredArgsConstructor
public class WebhookService {
    private final BotService botService;
    private final UserService userService;
    private final UtilService utilService;
    public void onUpdateToReceive(Update update) {
        BotState state = botService.getAndCheck(update);
        String chatId = utilService.getChatIdFromUpdate(update);
        Integer messageId = utilService.getMessageIdFromUpdate(update);
        Message message  = utilService.getMessageFromUpdate(update);
        String callBackData = (update.hasCallbackQuery()) ? update.getCallbackQuery().getData() : message.getText();
        System.out.println(state);
        if (update.hasMessage()) {
            if (message.hasText()) {
                String text = message.getText();
                if (text.equals("/start")) {
                    state = BotState.SEND_PHONE;
                }else if (text.equals(utilService.getTextByLanguage(chatId, Constant.SETTINGS))){
                    state = BotState.SEND_BTN_SETTING;
                }else if (text.equals(utilService.getTextByLanguage(chatId, Constant.CHANGE_TYPE_FILE))){
                    state = BotState.SEND_BTN_TYPE_FILE;
                }else if (text.equals(utilService.getTextByLanguage(chatId, Constant.CHANGE_LANGUAGE))){
                    state = BotState.SEND_BTN_CHANGE_LANG;
                }else if (text.equals(utilService.getTextByLanguage(chatId, Constant.BACK))){
                    state = BotState.SEND_BTN_MAIN_MANU;
                }else if (text.equals(utilService.getTextByLanguage(chatId, Constant.BALANCE))) {
                    state = BotState.GET_BALANCE;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.AKT_SVERKA_TOVAR))) {
                    state = BotState.GET_START_DATE;
                } else if (text.equals(utilService.getTextByLanguage(chatId, Constant.AKT_SVERKA))) {
                    state = BotState.GET_START_DATEV2;
                }else if (state == BotState.SEND_PHONE){
                    state = BotState.SAVE_NAME;
                }else if (UtilService.containsOnlyNumbers(text)){
                    state = BotState.GET_PRODUCT;
                }else if (state.toString().startsWith("EDIT2")){
                    state = BotState.SEND_CONTINUE;
                }
            } else if (message.hasContact()) {
                state = BotState.GET_CONTACT;
                userService.updateUserState(update, state, message.getContact().getPhoneNumber());
            }
        }

        if (update.hasCallbackQuery()) {
            if (utilService.getErrorDate(update)){
                state = BotState.SEND_EXCEPTION;
            }else {
                switch (callBackData) {
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
                };
                switch (state) {
                    case GET_START_DATE, GET_START_DATEV2 -> {
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
                        }else if (utilService.getMonth(update)) {
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
                        }else if (utilService.getMonth(update)) {
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
            case SAVE_NAME -> botService.saveName(message, chatId);
            case GET_START_DATE, GET_START_DATEV2 -> botService.sendStartDate(state, chatId);
            case SEND_CALENDAR -> botService.sendStartDateAsCalendar(message, chatId);
            case REPORTS, SEND_BY_MONTH, SEND_BY_DAY, SEND_BY_WEEK, SEND_BY_LAST_MONTH, SEND_BY_SEASON, SEND_BY_YEAR -> botService.getReport(state, chatId, messageId, callBackData);
            case SAVE_START_DATE-> botService.saveStartDate(message, chatId, callBackData);
            case SEND_EXCEPTION-> botService.sendError(chatId, messageId);
            case GET_BALANCE-> botService.getBalance(chatId);
            case EDIT_START_DATE, EDIT_END_DATE-> botService.editDate(chatId, messageId, callBackData);
            case SEND_BTN_SETTING-> botService.sendSettings(chatId);
            case SEND_BTN_TYPE_FILE-> botService.sendTypeFile(chatId);
            case SEND_BTN_CHANGE_LANG-> botService.sendLanguage(chatId);
            case SEND_BTN_MAIN_MANU-> botService.sendMainMenu(chatId);
            case SEND_CONTINUE-> botService.sendContinue(chatId);
            case EDIT2XLSX, EDIT2JPG, EDIT2PDF-> botService.editTypeFile(state, chatId, messageId, callBackData);
            case EDIT2UZ, EDIT2RU, EDIT2KRIL-> botService.editLanguage(state, chatId, messageId, callBackData);
        }
        if (state!=BotState.SAVE_NAME)
            if (state!=BotState.SEND_CALENDAR)
               botService.saveData(update, new UserDTO(state));
    }
}