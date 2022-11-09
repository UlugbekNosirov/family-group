package uz.murodjon.notificationbot.service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.murodjon.notificationbot.payload.UserDTO;
import uz.murodjon.notificationbot.utils.BotState;

import static uz.murodjon.notificationbot.utils.Constant.ENTER_MARKET_NAME_BTN;
import static uz.murodjon.notificationbot.utils.Constant.PASSWORD;

@Service
@RequiredArgsConstructor
public class WebhookService {
    private final BotService botService;
    private final UserService userService;

    public void onUpdateToReceive(Update update) {
        UserDTO userDTO = botService.getAndCheck(update);
        if (userDTO == null) return;
        BotState state = userDTO.getState();
        if (update.hasMessage()) {
            Message message = update.getMessage();
            if (message.hasText()) {
                String text = message.getText();
                if (text.equals("/start")) {
                    state = BotState.CHOOSE_MARKET;
                } else if (text.equals(PASSWORD)) {
                    state = BotState.SEND_MARKET_NAME;
                }else if (state==BotState.SEND_MARKET_NAME_TEXT){
                    state=BotState.SEND_USERNAME;
                } else if (state==BotState.SEND_USERNAME) {
                    state=BotState.GET_USERNAME;
                }
            } else if (message.hasContact()) {
                state = BotState.GET_CONTACT;
                userService.updateUserState(message.getChatId().toString(), state);
            }
        }
        if (update.hasCallbackQuery()) {
            if (update.getCallbackQuery().getData().equals(ENTER_MARKET_NAME_BTN))
                state = BotState.SEND_MARKET_NAME_TEXT;
            else
            state = BotState.SEND_PHONE;
        }
        switch (state) {
            case CHOOSE_MARKET -> botService.getMainMenuSend(update);
            case SEND_PHONE -> botService.saveChosenMarket(update);
            case GET_CONTACT -> botService.saveContact(update.getMessage().getChatId().toString(), update);
            case SEND_MARKET_NAME -> botService.createAddMarketMessage(update.getMessage().getChatId().toString());
            case SEND_MARKET_NAME_TEXT -> botService.enterMarketName(update);
            case SEND_USERNAME -> botService.saveMarketName(update);
            case GET_USERNAME -> botService.saveMarketUsername(update);
        }
        botService.saveData(update, new UserDTO(state));
    }
}
