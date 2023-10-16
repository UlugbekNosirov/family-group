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
        System.out.println(state);
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.hasText()) {
                String text = message.getText();
                if (text.equals("/start")) {
                    state = BotState.SEND_PHONE;
                }else if (text.equals("/settings")){
                    state = BotState.SEND_BTN_TYPE_FILE;
                }else if (state == BotState.SEND_PHONE){
                    state = BotState.SAVE_NAME;
                }else if (text.equals("\uD83D\uDCB0БАЛАНС")) {
                    state = BotState.GET_BALANCE;
                } else if (text.equals("\uD83D\uDCC5АКТ СВEРКА (товар)")) {
                    state = BotState.GET_START_DATE;
                } else if (text.equals("\uD83D\uDCC5АКТ СВEРКА")) {
                    state = BotState.GET_START_DATEV2;
                }else if (UtilService.containsOnlyNumbers(text)){
                    state = BotState.GET_PRODUCT;
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
                state = switch (callbackData) {
                    case "pdf" -> BotState.EDIT2PDF;
                    case "jpg" -> BotState.EDIT2JPG;
                    case "xlsx" -> BotState.EDIT2XLSX;
                    default -> state;
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
            case REPORTS, SEND_BY_MONTH -> botService.getReport(state, update);
            case SAVE_START_DATE-> botService.saveStartDate(update);
            case SEND_EXCEPTION-> botService.sendError(update);
            case TAKE_BALANCE, GET_BALANCE-> botService.getBalance(update);
            case EDIT_START_DATE, EDIT_END_DATE-> botService.editDate(update);
            case SEND_BTN_TYPE_FILE-> botService.sendTypeFile(update);
            case EDIT2XLSX, EDIT2JPG, EDIT2PDF-> botService.editTypeFile(state, update);
        }
        if (state!=BotState.SAVE_NAME)
            botService.saveData(update, new UserDTO(state));
    }
}