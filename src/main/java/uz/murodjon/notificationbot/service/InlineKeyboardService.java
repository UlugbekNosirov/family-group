package uz.murodjon.notificationbot.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import uz.murodjon.notificationbot.utils.Constant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
@Service
public class InlineKeyboardService implements Constant {
    public static InlineKeyboardMarkup createMarkup(List<List<String>> rows) {
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        for (List<String> row : rows) {
            List<InlineKeyboardButton> dRow = new ArrayList<>();
            for (String word : row) {
                InlineKeyboardButton button;
                if (!word.contains("BACK")) {
                    button = new InlineKeyboardButton(word);
                    button.setCallbackData(word);
                } else {
                    button = new InlineKeyboardButton(BACK);
                    button.setCallbackData(word);
                }
                dRow.add(button);
            }
            rowList.add(dRow);
        }

        return new InlineKeyboardMarkup(rowList);
    }

    public static InlineKeyboardMarkup createMarkupForAddMarket(){
        return createMarkup(List.of(List.of(ENTER_MARKET_NAME_BTN)));
    }
}
