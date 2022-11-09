package uz.murodjon.notificationbot.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.murodjon.notificationbot.model.Market;
import uz.murodjon.notificationbot.service.MarketService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static uz.murodjon.notificationbot.utils.Constant.MY_PHONE_NUMBER;

@Component
@RequiredArgsConstructor
public class Keyboard {

private final MarketService marketService;
    public InlineKeyboardMarkup createInlineMarkupForMarkets() {
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        List<Market> markets =  marketService.findAll();
        for (int i = 1; i < markets.size()+1; i++) {
            Market mar = markets.get(i-1);
            InlineKeyboardButton btn = new InlineKeyboardButton(mar.getName());
            btn.setCallbackData(mar.getId().toString());
            row.add(btn);
            if (i % 3 == 0) {
                rowList.add(row);
                row = new ArrayList<>();
            }
        }
        if (!row.isEmpty())
            rowList.add(row);
//        if (Objects.nonNull(back))
//            rowList.add(List.of(getInlineButton(back, BACK)));
        return new InlineKeyboardMarkup(rowList);
    }

    public ReplyKeyboardMarkup createContactMarkup() {
        KeyboardButton contactBtn = new KeyboardButton(MY_PHONE_NUMBER);
        contactBtn.setRequestContact(true);
      return   new ReplyKeyboardMarkup(List.of(new KeyboardRow(List.of(contactBtn))), true, false, true,"next");

    }

}
