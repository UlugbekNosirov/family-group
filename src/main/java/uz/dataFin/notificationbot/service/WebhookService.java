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
    public void onUpdateToReceive(Update update) {
        BotState state = botService.getAndCheck(update);
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.hasText()) {
                String text = message.getText();
                if (text.equals("/start")) {
                    state = BotState.SEND_PHONE;
                }else if (state == BotState.GET_NAME){
                    state = BotState.SAVE_NAME;
                }else if (text.equals("БАЛАНС")) {
                    state = BotState.GET_BALANCE;
                } else if (text.equals("АКТ СВEРКА (товар)")) {
                    state = BotState.GET_START_DATE;
                } else if (text.equals("АКТ СВEРКА")) {
                    state = BotState.GET_START_DATEV2;
                }else if (containsOnlyNumbers(text)){
                    state = BotState.GET_PRODUCT;
                } else if (state == BotState.ALREADY_REGISTERED) {
                    state = BotState.GET_CONTACT;
                } else if (state == BotState.GET_CONTACT) {
                    state = BotState.GUIDE;
                }else if (state == BotState.GET_BALANCE) {
                    state = BotState.TAKE_BALANCE;
                }
            } else if (message.hasContact()) {
                state = BotState.GET_CONTACT;
                userService.updateUserState(message.getChatId().toString(), state, message.getContact().getPhoneNumber());
            }
        }

        //////////////////////////////////////

        if (update.hasCallbackQuery()) {
            if (getErrorDate(update)){
                state = BotState.SEND_EXCEPTION;
            }else {
                String callbackData = update.getCallbackQuery().getData();
                switch (state) {
                    case GET_START_DATE, GET_START_DATEV2 -> {
                        if (!checkBADate(callbackData) && !getMonth(update)) {
                            state = BotState.SAVE_START_DATE;
                        } else if (checkBADate(callbackData)) {
                            state = BotState.EDIT_START_DATE;
                        } else if (getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        }
                    }
                    case EDIT_START_DATE -> {
                        if (!checkBADate(callbackData) && !getMonth(update)) {
                            state = BotState.SAVE_START_DATE;
                        }else if (getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        }
                    }
                    case SAVE_START_DATE -> {
                        if (!checkBADate(callbackData) && !getMonth(update)) {
                            state = BotState.REPORTS;
                        } else if (checkBADate(callbackData)) {
                            state = BotState.EDIT_END_DATE;
                        } else if (getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        }
                    }
                    case EDIT_END_DATE -> {
                        if (!checkBADate(callbackData) && getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        } else if (!checkBADate(callbackData) && !getMonth(update)) {
                            state = BotState.REPORTS;
                        }else if (getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        }
                    }
                }
                if (callbackData.equals(Constant.ENTER_MARKET_NAME_BTN)) {
                    state = BotState.SEND_MARKET_NAME_TEXT;
                }
                System.out.println(callbackData);
            }
        }


        switch (state) {
            case SEND_PHONE, GET_CONTACT -> botService.getMainMenuSend(state, update);
            case GUIDE -> botService.saveContact(update.getMessage().getChatId().toString(), update);
            case GET_PRODUCT -> botService.Employee(update);
            case GET_NAME -> botService.sendName(update);
            case SAVE_NAME -> botService.saveName(update);
            case GET_START_DATE, GET_START_DATEV2 -> botService.sendStartDate(state, update);
            case REPORTS, SEND_BY_MONTH -> botService.getReport(state, update);
            case SAVE_START_DATE-> botService.saveStartDate(state, update);
            case SEND_EXCEPTION-> botService.sendError(update);
            case TAKE_BALANCE, GET_BALANCE-> botService.getBalance(update);
            case EDIT_START_DATE, EDIT_END_DATE-> botService.editDate(update);

        }
        botService.saveData(update, new UserDTO(state));
    }
    public Boolean checkBADate(String inlineDate){
        String inD[]=inlineDate.split("-");
        return inD.length==2;
    }

    public boolean getMonth(Update update){
        String data = update.getCallbackQuery().getData();
        String inD[]=data.split("-");
        return inD.length==1;
    }

    public Boolean getErrorDate(Update update){
        return update.getCallbackQuery().getData().startsWith("calendarNone");
    }

    public static boolean containsOnlyNumbers(String text) {
        text = text.trim();
        if (text.isEmpty()) {
            return false;
        }
        for (char c : text.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }



}
