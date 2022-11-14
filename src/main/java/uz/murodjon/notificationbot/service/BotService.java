package uz.murodjon.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import uz.murodjon.notificationbot.feign.TelegramFeign;
import uz.murodjon.notificationbot.helper.Keyboard;
import uz.murodjon.notificationbot.model.Balance;
import uz.murodjon.notificationbot.model.Market;
import uz.murodjon.notificationbot.model.MessageDTO;
import uz.murodjon.notificationbot.model.Users;
import uz.murodjon.notificationbot.payload.UserDTO;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static uz.murodjon.notificationbot.utils.Constant.*;

@Service
@RequiredArgsConstructor
public class BotService {
    private final TelegramFeign feign;
    private final UserService userService;
    private final BalanceService balanceService;
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
        Users user = userService.getByChatId(messageDTO.getChatId());
        Market market = marketService.getMarketByUserName(username);
        balanceService.saveBalance(messageDTO, market, user);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(messageDTO.getChatId());
        sendMessage.setText("\uD83C\uDFEA "+market.getName() + "\n" + messageDTO.getText());
        feign.sendMessage(sendMessage);
    }

    public void getBalance(Update update) {
        Users user = userService.getByChatId(update.getMessage().getChatId().toString());
        List<Balance> balances = balanceService.findByUsers(user);
        String text = "";
        if (balances!=null){
            for (Balance balance : balances) {
                text += "\uD83C\uDFEA "+balance.getMarket().getName()+"\n \n"+
                        "\uD83D\uDCC6 Сана: "+LocalDate.now()+"\n"+
                        "\uD83D\uDC68\u200D\uD83D\uDCBC Мижоз: "+balance.getUsers().getFirstname()+
                        " "+balance.getUsers().getLastname()+"\n"+
                        "\uD83C\uDFC1 Колдик бонус: "+(balance.getBonus()==null?0:balance.getBonus())+"\n"+
                        "\uD83D\uDCB8 Баланс (UZS): "+(balance.getSum()==null?0:balance.getSum())+"\n"+
                        "\uD83D\uDCB2 Баланс (USD): "+(balance.getDollar()==null?0:balance.getDollar())+"\n";
            }
            if (text.equals("")){
                text = "Афсуски хозирда сизда хеч кандай баланс хакида маълумот йук";
            }
        }else{
            text = "Афсуски хозирда сизда хеч кандай баланс хакида маълумот йук";
        }
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId().toString());
        sendMessage.setText(text);
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
