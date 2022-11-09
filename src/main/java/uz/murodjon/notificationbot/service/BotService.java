package uz.murodjon.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import uz.murodjon.notificationbot.feign.TelegramFeign;
import uz.murodjon.notificationbot.helper.Keyboard;
import uz.murodjon.notificationbot.model.Market;
import uz.murodjon.notificationbot.model.MessageDTO;
import uz.murodjon.notificationbot.model.Users;
import uz.murodjon.notificationbot.payload.UserDTO;
import uz.murodjon.notificationbot.utils.BotState;

import static uz.murodjon.notificationbot.utils.Constant.*;

@Service
@RequiredArgsConstructor
public class BotService {
    private final TelegramFeign feign;
    private final UserService userService;
    public final MarketService marketService;
    private final Keyboard keyboard;
    private final InlineKeyboardService inlineKeyboardService;

    public UserDTO getAndCheck(Update update) {
        return userService.checkAndGet(update);
    }


    public void saveData(Update update, UserDTO userDTO) {
        Message message = update.hasMessage() ? update.getMessage() : update.getCallbackQuery().getMessage();
        userService.saveData(message.getChatId().toString(), userDTO);
    }

    public void getMainMenuSend(Update update) {
        Message message = update.hasMessage() ? update.getMessage() : update.getCallbackQuery().getMessage();
        SendMessage sendMessage = new SendMessage(message.getChatId().toString(), WELCOME);
        sendMessage.setReplyMarkup(keyboard.createInlineMarkupForMarkets());
        feign.sendMessage(sendMessage);
    }

    public void saveChosenMarket(Update update) {
        Message message = update.hasMessage() ? update.getMessage() : update.getCallbackQuery().getMessage();
        String chatId = message.getChatId().toString();
        Market market = marketService.getMarket(new Long(update.getCallbackQuery().getData()));
        userService.saveMarket(market, chatId);

        Users user = userService.getByChatId(chatId);
        if (user.getPhone() == null)
            feign.sendMessage(createContactMessage(chatId));
        else feign.sendMessage(new SendMessage(message.getChatId().toString(), SUCCESS_MESSAGE));
    }

    public void sendMessageToUser(MessageDTO messageDTO, String username) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(messageDTO.getChatId());
        sendMessage.setText(username + " : " + messageDTO.getText());
        feign.sendMessage(sendMessage);
    }

    public SendMessage createContactMessage(String chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(GIVE_PHONE_TEXT);
        sendMessage.setReplyMarkup(keyboard.createContactMarkup());
        return sendMessage;
    }

    public void saveContact(String chatId, Update update) {
        Users user = userService.getByChatId(chatId);
        user.setPhone(update.getMessage().getContact().getPhoneNumber());
        userService.saveUser(user);
        SendMessage sendMessage = new SendMessage(chatId, SUCCESS_MESSAGE);
        sendMessage.setReplyMarkup(new ReplyKeyboardRemove(true));
        feign.sendMessage(sendMessage);
    }

    public void saveMarketName(Update update) {
        marketService.saveMarketName(update.getMessage().getText());
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId().toString());
        sendMessage.setText(ENTER_MARKET_USERNAME);
        feign.sendMessage(sendMessage);
    }

    public void saveMarketUsername(Update update) {
        marketService.saveMarketUsername(update.getMessage().getText());
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId().toString());
        sendMessage.setText(SUCCESS_MESSAGE);
        feign.sendMessage(sendMessage);
    }

    public void createAddMarketMessage(String chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(CHOOSE_ACTION);
        sendMessage.setReplyMarkup(InlineKeyboardService.createMarkupForAddMarket());
        feign.sendMessage(sendMessage);
    }


    public void enterMarketName(Update update) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId().toString());
        sendMessage.setText(ENTER_MARKET_NAME);
        feign.sendMessage(sendMessage);
    }
}
