package uz.murodjon.notificationbot.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
@RequiredArgsConstructor
public class Sender {
    private final Keyboard keyboard;

    public SendMessage sendMessageWithServices(String chatId) {
        SendMessage sendMessage = new SendMessage(chatId, "WELCOME");
        sendMessage.setReplyMarkup(keyboard.createInlineMarkupForMarkets());
        return sendMessage;
    }
}
