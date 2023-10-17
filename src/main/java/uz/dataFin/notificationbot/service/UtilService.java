package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;


@Service
@RequiredArgsConstructor
public class UtilService {
    private final static String words ="abdefghijklmnopqrstuvxyzwcйцукенгшўзхъфқвапролджэячсмитьбюёҳғыщ1234567890.,/*-+_:!?@#$%^&()'\"[]{}|<>'\'№~` ";
    private final static String[] months = {"Yanvar", "Fevral", "Mart", "Aprel", "May", "Iyun", "Iyul", "Avgust", "Sentabr", "Oktyabr", "Noyabr", "Dekabr"};

    public static LocalDate[] getFirstAndLastDayOfMonth(String monthName) {
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

    Path checkPackage(Path file) {
        if (!file.toFile().exists())
            file.toFile().mkdirs();
        return file;
    }
    public LocalDate setDate(Update update) {
        String inlineDate = update.getCallbackQuery().getData();
        String[] inD = inlineDate.split("-");
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

    public static boolean containsSpecialCharacters(String text) {
        String lowerCase = text.toLowerCase();
        for (char c : lowerCase.toCharArray()) {
            if (words.indexOf(c) == -1) {
                return true;
            }
        }
        return false;
    }
}
