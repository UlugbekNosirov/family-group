package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import uz.dataFin.notificationbot.feign.TelegramFeign;
import uz.dataFin.notificationbot.model.Users;
import uz.dataFin.notificationbot.repository.UserRepository;
import uz.dataFin.notificationbot.utils.BotState;


import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.Objects;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class UtilService {
    private final UserRepository userRepository;
    private final TelegramFeign feign;

    private final static String words ="abdefghijklmnopqrstuvxyzwcйцукенгшўзхъфқвапролджэячсмитьбюёҳғыщ1234567890.,/*-+_:!?@#$%^&()'\"[]{}|<>'\'№~` '";
    private final static String[] months = {"Yanvar", "Fevral", "Mart", "Aprel", "May", "Iyun", "Iyul", "Avgust", "Sentabr", "Oktyabr", "Noyabr", "Dekabr"};
    public LocalDate[] getFirstAndLastDayOfMonth(String monthName) {
        Month month = null;
        for (String mName : months) {
            if (mName.equals(monthName)) {
                month = convertToMonthEnum(mName);
                break;
            }
        }
        try {
            assert month != null;
            LocalDate firstDay = LocalDate.now().withMonth(month.getValue()).withDayOfMonth(1);
            LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
            return new LocalDate[]{firstDay, lastDay};
        } catch (Exception e) {
            System.out.println("Month'ni convert qilishda xatolik" + e);
        }
        return null;
    }

    public String getTextByLanguage(String chatId, String text) {
        try {
            Optional<Users> users = userRepository.findByChatId(chatId);
            String[] str = text.split("/");
            if (users.isPresent()) {
                String language = users.get().getLanguage();
                if (language.equals("uz"))
                    return str[2];
                else if (language.equals("kril")) {
                    return str[0];
                }
            }
            return str[1];
        }catch (Exception e){
            return text;
        }
    }


    public LocalDate[] getBeginningOfWeekAndToday() {
        LocalDate today = LocalDate.now();
        DayOfWeek currentDayOfWeek = today.getDayOfWeek();
        LocalDate beginningOfWeek = today.minusDays(currentDayOfWeek.getValue() - DayOfWeek.MONDAY.getValue());
        return new LocalDate[]{beginningOfWeek, today};
    }
    public String getChatIdFromUpdate(Update update){
        if (update.hasMessage()){
            return update.getMessage().getChatId().toString();
        }else if (update.hasCallbackQuery()){
            return update.getCallbackQuery().getMessage().getChatId().toString();
        }else {
            System.out.println("chatId ni olishda xatolik");
        }
        return "1148134936";
    }

    public Integer getMessageIdFromUpdate(Update update){
        if (update.hasMessage()){
            return update.getMessage().getMessageId();
        }else if (update.hasCallbackQuery()){
            return update.getCallbackQuery().getMessage().getMessageId();
        }else {
            System.out.println("chatId ni olishda xatolik");
        }
        return 1148134936;
    }

    public static LocalDate[] getFirstDayOfMonthAndToday() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = LocalDate.of(today.getYear(), today.getMonth(), 1);

        return new LocalDate[]{ firstDayOfMonth, today };
    }
    public Message getMessageFromUpdate(Update update){
        if (update.hasMessage()){
            return update.getMessage();
        }else if (update.hasCallbackQuery()){
            return update.getCallbackQuery().getMessage();
        }else {
            System.out.println("chatId ni olishda xatolik");
        }
        return new Message();
    }

    public static LocalDate[] getFirstDayOfQuarterAndToday() {
        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        LocalDate firstDayOfQuarter;

        if (currentMonth <= 3) {
            firstDayOfQuarter = LocalDate.of(today.getYear(), Month.JANUARY, 1);
        } else if (currentMonth <= 6) {
            firstDayOfQuarter = LocalDate.of(today.getYear(), Month.APRIL, 1);
        } else if (currentMonth <= 9) {
            firstDayOfQuarter = LocalDate.of(today.getYear(), Month.JULY, 1);
        } else {
            firstDayOfQuarter = LocalDate.of(today.getYear(), Month.OCTOBER, 1);
        }
        return new LocalDate[]{ firstDayOfQuarter, today };
    }

    public static LocalDate[] getFirstDayOfYearAndToday() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfYear = LocalDate.of(today.getYear(), 1, 1);

        return new LocalDate[]{ firstDayOfYear, today };
    }

    Path checkPackage(Path file) {
        if (!file.toFile().exists())
            file.toFile().mkdirs();
        return file;
    }
    public LocalDate setDate(String callBackData) {
        String[] inD = callBackData.split("-");
        int year = Integer.parseInt(inD[0]);
        int month = Integer.parseInt(inD[1]);
        int day = 1;
        return LocalDate.of(year, month, day);
    }

    public static Month convertToMonthEnum(String monthName) {
        return switch (monthName) {
            case "Yanvar" -> Month.JANUARY;
            case "Fevral" -> Month.FEBRUARY;
            case "Mart" -> Month.MARCH;
            case "Aprel" -> Month.APRIL;
            case "May" -> Month.MAY;
            case "Iyun" -> Month.JUNE;
            case "Iyul" -> Month.JULY;
            case "Avgust" -> Month.AUGUST;
            case "Sentabr" -> Month.SEPTEMBER;
            case "Oktyabr" -> Month.OCTOBER;
            case "Noyabr" -> Month.NOVEMBER;
            case "Dekabr" -> Month.DECEMBER;
            default -> throw new IllegalArgumentException("Invalid month name: " + monthName);
        };
    }

    public Boolean checkBADate(String inlineDate){
        String[] inD =inlineDate.split("-");
        return inD.length==2;
    }

    public boolean getMonth(Update update){
        String data = update.getCallbackQuery().getData();
        String[] inD =data.split("-");
        return inD.length==1;
    }

    public Boolean getErrorDate(Update update){
        return update.getCallbackQuery().getData().startsWith("calendarNone");
    }

    public boolean containsSpecialCharacters(String text) {
        String lowerCase = text.toLowerCase();
        for (char c : lowerCase.toCharArray()) {
            if (words.indexOf(c) == -1 || text.length()<4) {
                return true;
            }
        }
        return false;
    }

    public void sendMessage(String chatId, String text, ReplyKeyboardMarkup replyKeyboardMarkup, InlineKeyboardMarkup inlineKeyboardMarkup){
        try {
            SendMessage sendMessage = new SendMessage(chatId, text);
            if (Objects.nonNull(replyKeyboardMarkup))
                sendMessage.setReplyMarkup(replyKeyboardMarkup);
            else
                sendMessage.setReplyMarkup(inlineKeyboardMarkup);

            feign.sendMessage(sendMessage);
        } catch (Exception e){
            System.out.println(e+"\n\nSendMessage error");
        }

    }

    public void editMessageText(String chatId, Integer messageId, String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
        try {
            EditMessageText editMessageText = new EditMessageText();
            editMessageText.setChatId(chatId);
            editMessageText.setMessageId(messageId);
            editMessageText.setText(text);
            if (Objects.nonNull(inlineKeyboardMarkup))
                editMessageText.setReplyMarkup(inlineKeyboardMarkup);
            feign.editMessageText(editMessageText);
        }catch (Exception e){
            System.out.println("Error editing message text\n\n\n");
        }
    }

    public String getPreviousStep(BotState step, String[] ReportAKTSteps) {
        for (int i = 1; i < ReportAKTSteps.length; i++) {
            if (ReportAKTSteps[i].equals(step.name())) {
                if (ReportAKTSteps[i].startsWith("GET_PRODUCT_"))
                    return ReportAKTSteps[1];
                return ReportAKTSteps[i - 1];
            }
        }
        return ReportAKTSteps[0];
    }

}
