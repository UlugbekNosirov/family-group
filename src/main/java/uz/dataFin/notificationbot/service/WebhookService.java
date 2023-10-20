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
        System.out.println(state);
        if (update.hasMessage()) {
            Message message = update.getMessage();
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
                String callbackData = update.getCallbackQuery().getData();
                switch (callbackData) {
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
                        if (!utilService.checkBADate(callbackData) && !utilService.getMonth(update)) {
                            state = BotState.SAVE_START_DATE;
                        } else if (utilService.checkBADate(callbackData)) {
                            state = BotState.EDIT_START_DATE;
                        } else if (utilService.getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        }
                    }
                    case EDIT_START_DATE -> {
                        if (!utilService.checkBADate(callbackData) && !utilService.getMonth(update)) {
                            state = BotState.SAVE_START_DATE;
                        }else if (utilService.getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        }
                    }
                    case SAVE_START_DATE -> {
                        if (!utilService.checkBADate(callbackData) && !utilService.getMonth(update)) {
                            state = BotState.REPORTS;
                        } else if (utilService.checkBADate(callbackData)) {
                            state = BotState.EDIT_END_DATE;
                        } else if (utilService.getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        }
                    }
                    case EDIT_END_DATE -> {
                        if (!utilService.checkBADate(callbackData) && utilService.getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        } else if (!utilService.checkBADate(callbackData) && !utilService.getMonth(update)) {
                            state = BotState.REPORTS;
                        }else if (utilService.getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        }
                    }
                }
                if (callbackData.equals(Constant.ENTER_MARKET_NAME_BTN)) {
                    state = BotState.SEND_MARKET_NAME_TEXT;
                }
            }
        }


        switch (state) {
            case SEND_PHONE, GET_CONTACT -> botService.getMainMenuSend(update);
            case GET_PRODUCT -> botService.Employee(update);
            case SAVE_NAME -> botService.saveName(update);
            case GET_START_DATE, GET_START_DATEV2 -> botService.sendStartDate(state, update);
            case SEND_CALENDAR -> botService.sendStartDateAsCalendar(update);
            case REPORTS, SEND_BY_MONTH, SEND_BY_DAY, SEND_BY_WEEK, SEND_BY_LAST_MONTH, SEND_BY_SEASON, SEND_BY_YEAR -> botService.getReport(state, update);
            case SAVE_START_DATE-> botService.saveStartDate(update);
            case SEND_EXCEPTION-> botService.sendError(update);
            case GET_BALANCE-> botService.getBalance(update);
            case EDIT_START_DATE, EDIT_END_DATE-> botService.editDate(update);
            case SEND_BTN_SETTING-> botService.sendSettings(update);
            case SEND_BTN_TYPE_FILE-> botService.sendTypeFile(update);
            case SEND_BTN_CHANGE_LANG-> botService.sendLanguage(update);
            case SEND_BTN_MAIN_MANU-> botService.sendMainMenu(update);
            case SEND_CONTINUE-> botService.sendContinue(update);
            case EDIT2XLSX, EDIT2JPG, EDIT2PDF-> botService.editTypeFile(state, update);
            case EDIT2UZ, EDIT2RU, EDIT2KRIL-> botService.editLanguage(state, update);
        }
        if (state!=BotState.SAVE_NAME)
            if (state!=BotState.SEND_CALENDAR)
               botService.saveData(update, new UserDTO(state));
    }
}