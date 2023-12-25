package uz.dataFin.notificationbot.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.dataFin.notificationbot.model.Market;
import uz.dataFin.notificationbot.model.Products;
import uz.dataFin.notificationbot.model.WarehouseDTO;
import uz.dataFin.notificationbot.service.*;
import uz.dataFin.notificationbot.utils.Constant;

import java.security.Key;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class Keyboard {

    private final MarketService marketService;
    private final ProductService productService;
    private final Api1CService api1CService;
    private final UtilService utilService;
    private final String months[] = {"Yanvar", "Fevral", "Mart", "Aprel", "May", "Iyun", "Iyul", "Avgust", "Sentabr", "Oktyabr", "Noyabr", "Dekabr"};

    public InlineKeyboardMarkup createInlineMarkupForMarkets() {
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        List<Market> markets = marketService.findAll();
        for (int i = 1; i < markets.size() + 1; i++) {
            Market mar = markets.get(i - 1);
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
        return new InlineKeyboardMarkup(rowList);
    }

    public InlineKeyboardMarkup typeFileButtons(String chatId) {
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton btn1 = new InlineKeyboardButton();
        btn1.setText(utilService.getTextByLanguage(chatId, Constant.IMAGE));
        btn1.setCallbackData("jpg");
        InlineKeyboardButton btn2 = new InlineKeyboardButton();
        btn2.setText("Excel");
        btn2.setCallbackData("xlsx");
        InlineKeyboardButton btn3 = new InlineKeyboardButton();
        btn3.setText("Pdf");
        btn3.setCallbackData("pdf");
        row.add(btn1);
        row.add(btn2);
        row2.add(btn3);
        rowList.add(row);
        rowList.add(row2);
        return new InlineKeyboardMarkup(rowList);
    }

    public InlineKeyboardMarkup chooseLanguage() {
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton btn1 = new InlineKeyboardButton();
        btn1.setText("\uD83C\uDDFA\uD83C\uDDFFLotincha");
        btn1.setCallbackData("uz");
        InlineKeyboardButton btn2 = new InlineKeyboardButton();
        btn2.setText("\uD83C\uDDF7\uD83C\uDDFAРусский");
        btn2.setCallbackData("ru");
        InlineKeyboardButton btn3 = new InlineKeyboardButton();
        btn3.setText("\uD83C\uDDFA\uD83C\uDDFF/\uD83C\uDDF7\uD83C\uDDFAКрилча");
        btn3.setCallbackData("kril");
        row.add(btn1);
        row.add(btn2);
        row2.add(btn3);
        rowList.add(row);
        rowList.add(row2);
        return new InlineKeyboardMarkup(rowList);
    }

    public InlineKeyboardMarkup Query(String chatId) {
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton btn1 = new InlineKeyboardButton();
        btn1.setText(utilService.getTextByLanguage(chatId, Constant.YES));
        btn1.setCallbackData("agreed");
        InlineKeyboardButton btn2 = new InlineKeyboardButton();
        btn1.setText(utilService.getTextByLanguage(chatId, Constant.YES));
        btn2.setText(utilService.getTextByLanguage(chatId, Constant.NO));
        btn2.setCallbackData("rejected");
        row.add(btn1);
        row.add(btn2);
        rowList.add(row);
        return new InlineKeyboardMarkup(rowList);
    }

    public InlineKeyboardMarkup periodKeyboards(String chatId) {
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton btn1 = new InlineKeyboardButton();
        btn1.setText(utilService.getTextByLanguage(chatId, Constant.TODAY));
        btn1.setCallbackData("day");
        InlineKeyboardButton btn2 = new InlineKeyboardButton();
        btn2.setText(utilService.getTextByLanguage(chatId, Constant.WEEK));
        btn2.setCallbackData("week");
        InlineKeyboardButton btn3 = new InlineKeyboardButton();
        btn3.setText(utilService.getTextByLanguage(chatId, Constant.MONTH));
        btn3.setCallbackData("month");
        InlineKeyboardButton btn4 = new InlineKeyboardButton();
        btn4.setText(utilService.getTextByLanguage(chatId, Constant.QUARTER));
        btn4.setCallbackData("season");
        InlineKeyboardButton btn5 = new InlineKeyboardButton();
        btn5.setText(utilService.getTextByLanguage(chatId, Constant.YEAR));
        btn5.setCallbackData("year");
        InlineKeyboardButton btn6 = new InlineKeyboardButton();
        btn6.setText(utilService.getTextByLanguage(chatId, Constant.OTHER));
        btn6.setCallbackData("other");
        row.add(btn1);
        row.add(btn2);
        row2.add(btn3);
        row2.add(btn4);
        row3.add(btn5);
        row3.add(btn6);
        rowList.add(row);
        rowList.add(row2);
        rowList.add(row3);
        return new InlineKeyboardMarkup(rowList);
    }

    public ReplyKeyboardMarkup createContactMarkup() {
        KeyboardButton contactBtn = new KeyboardButton(Constant.MY_PHONE_NUMBER);
        contactBtn.setRequestContact(true);
        return new ReplyKeyboardMarkup(List.of(new KeyboardRow(List.of(contactBtn))), true, false, true, "next");
    }

    public ReplyKeyboardMarkup startBtn() {
        KeyboardButton contactBtn = new KeyboardButton("/start");
        return new ReplyKeyboardMarkup(List.of(new KeyboardRow(List.of(contactBtn))), true, false, true, "next");
    }

    public ReplyKeyboardMarkup panelBtns(String role, String chatId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        KeyboardRow row = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();
        KeyboardRow row3 = new KeyboardRow();
        KeyboardRow row4 = new KeyboardRow();
        List<KeyboardRow> rowList = new ArrayList<>();
        markup.setOneTimeKeyboard(false);
        markup.setResizeKeyboard(true);
        markup.setSelective(true);
        if (role.equals("ADMIN")){
            row3.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.WAREHOUSE)));
            row3.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.TRADE)));
            row4.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.ACCOUNT_DEBT)));
            row4.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.CASH_BOX)));
            rowList.add(row3);
            rowList.add(row4);
        }
        row.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.AKT_SVERKA)));
        row.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.AKT_SVERKA_TOVAR)));
        row2.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.BALANCE)));
        row2.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.SETTINGS)));
        rowList.add(row);
        rowList.add(row2);
        markup.setKeyboard(rowList);
        return markup;
    }

    public ReplyKeyboard wareHouse(String chatId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        List<KeyboardRow> rowList = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Faqat davr filtri bilan hisobiotni ko'rish"));
        markup.setOneTimeKeyboard(false);
        markup.setResizeKeyboard(true);
        markup.setSelective(true);
        WarehouseDTO[] warehouseDTO = api1CService.getWarehouses(chatId);
        for (WarehouseDTO dto : warehouseDTO) {
            KeyboardRow row = new KeyboardRow();
            row.add(new KeyboardButton(dto.getName()));
            rowList.add(row);
        }
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("\uD83D\uDD19Orqaga"));
        row.add(new KeyboardButton("Tovar tanlash"));
        rowList.add(row);
        return null;
    }
    public ReplyKeyboardMarkup reportBtn(String chatId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        KeyboardRow row = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();
        KeyboardRow row3 = new KeyboardRow();
        KeyboardRow row4 = new KeyboardRow();
        List<KeyboardRow> rowList = new ArrayList<>();
        markup.setOneTimeKeyboard(false);
        markup.setResizeKeyboard(true);
        markup.setSelective(true);
        row.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.AKT_SVERKA)));
        row.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.AKT_SVERKA)));
        row2.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.ACCOUNT_DEBT)));
        row2.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.WAREHOUSE)));
        row3.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.COST)));
        row3.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.TRADE)));
        row4.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.BALANCE)));
        row4.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.SETTINGS)));
        rowList.add(row);
        rowList.add(row2);
        rowList.add(row3);
        rowList.add(row4);
        markup.setKeyboard(rowList);
        return markup;
    }

    public ReplyKeyboardMarkup settings(String chatId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        KeyboardRow row = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();
        List<KeyboardRow> rowList = new ArrayList<>();
        markup.setOneTimeKeyboard(false);
        markup.setResizeKeyboard(true);
        markup.setSelective(true);
        row.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.CHANGE_LANGUAGE)));
        row.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.CHANGE_TYPE_FILE)));
        row2.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.BACK)));
        rowList.add(row);
        rowList.add(row2);
        markup.setKeyboard(rowList);
        return markup;
    }

    public ReplyKeyboardMarkup nullBtn(String chatId, String clientId) {
        List<Products> all = productService.findAll(clientId);
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        KeyboardRow row = new KeyboardRow();
        KeyboardRow row1 = new KeyboardRow();
        KeyboardRow row2 = new KeyboardRow();
        List<KeyboardRow> rowList = new ArrayList<>();
        markup.setOneTimeKeyboard(false);
        markup.setResizeKeyboard(true);
        markup.setSelective(true);
        row1.add(new KeyboardButton(utilService.getTextByLanguage(chatId, Constant.BALANCE)));
        for (int i = 1; i < all.size() + 1; i++) {
            if (i == 5) {
                break;
            }
            Products products = all.get(i - 1);
            if (i % 2 == 0) {
                row.add(new KeyboardButton(products.getCode()));
            } else {
                row2.add(new KeyboardButton(products.getCode()));
            }
        }
        rowList.add(row1);
        rowList.add(row2);
        rowList.add(row);
        markup.setKeyboard(rowList);
        return markup;
    }

    public InlineKeyboardMarkup calendarBtns(String inlineDate) {
        String yearAndMonth[] = inlineDate.split("-");
        int inlineYear = Integer.parseInt(yearAndMonth[0]);
        int inlineMonth = Integer.parseInt(yearAndMonth[1]);

        int dayOfWeek = checkDayOfWeek(inlineYear, inlineMonth);
        int daysOfMonth = countDaysOfMonth(inlineYear, inlineMonth);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        String beforeDate = "";
        if (inlineMonth != 1) {
            beforeDate = "" + inlineYear + "-" + (inlineMonth - 1) + "";
        } else {
            beforeDate = "" + (inlineYear - 1) + "-12";
        }

        String afterDate = "";
        if (inlineMonth != 12) {
            afterDate = "" + inlineYear + "-" + (inlineMonth + 1) + "";
        } else {
            afterDate = "" + (inlineYear + 1) + "-1";
        }
        rowInline.add(button("<", beforeDate));
        rowInline.add(button(inlineDate, "calendarNoneDate"));
        rowInline.add(button(">", afterDate));

        rowsInline.add(rowInline);

        rowInline = new ArrayList<>();
        rowInline.add(button(this.months[inlineMonth - 1], this.months[inlineMonth - 1]));

        rowsInline.add(rowInline);

        rowInline = new ArrayList<>();
        rowInline.add(button("D", "calendarNoneD"));
        rowInline.add(button("S", "calendarNoneS"));
        rowInline.add(button("Ch", "calendarNoneCh"));
        rowInline.add(button("P", "calendarNoneP"));
        rowInline.add(button("J", "calendarNoneJ"));
        rowInline.add(button("Sh", "calendarNoneSh"));
        rowInline.add(button("Y", "calendarNoneY"));

        rowsInline.add(rowInline);

        rowInline = new ArrayList<>();
        int isEnd = 1;
        int day = 1;
        while (true) {
            if (isEnd != 0) {

                for (int i = 0; i < 7; i++) {

                    if (isEnd == 1) {
                        if (i != dayOfWeek) {
                            rowInline.add(button(" ", "calendarNoneB" + i));
                        } else {
                            rowInline.add(
                                    button("" + day + "", "" + inlineYear
                                            + "-" + (inlineMonth < 10 ? "0" + inlineMonth : inlineMonth)
                                            + "-" + (day < 10 ? "0" + day : day) + ""));
                            day += 1;
                            isEnd = 2;
                        }
                    } else {
                        if (day > daysOfMonth) {
                            rowInline.add(button(" ", "calendarNoneE" + i));
                            isEnd = 0;
                        } else {
                            rowInline.add(button("" + day + "", "" + inlineYear
                                    + "-" + (inlineMonth < 10 ? "0" + inlineMonth : inlineMonth)
                                    + "-" + (day < 10 ? "0" + day : day) + ""));
                            day += 1;
                        }
                    }
                }
                rowsInline.add(rowInline);
                rowInline = new ArrayList<>();
            } else {
                break;
            }
        }
        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }

    public Integer countDaysOfMonth(Integer year, Integer month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        return yearMonth.lengthOfMonth();
    }

    public Integer checkDayOfWeek(Integer year, Integer month) {
        Date date = new Date(year - 1900, month - 1, 0);
        return date.getDay();
    }

    public InlineKeyboardButton button(String text, String callBackDate) {
        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        inlineKeyboardButton.setCallbackData(callBackDate);
        inlineKeyboardButton.setText(text);
        return inlineKeyboardButton;
    }


}
