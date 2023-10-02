package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import uz.dataFin.notificationbot.feign.TelegramFeign;
import uz.dataFin.notificationbot.helper.Keyboard;
import uz.dataFin.notificationbot.model.*;
import uz.dataFin.notificationbot.payload.UserDTO;
import uz.dataFin.notificationbot.utils.BotState;
import uz.dataFin.notificationbot.utils.Constant;
import uz.dataFin.notificationbot.utils.Security;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BotService {
    private final TelegramFeign feign;
    private final UserService userService;
    private final BalanceService balanceService;
    public final MarketService marketService;
    public final FileService fileService;
    public final ProductService productService;
    private final Keyboard keyboard;

    private final static String months[] = {"Yanvar", "Fevral", "Mart", "Aprel", "May", "Iyun", "Iyul", "Avgust", "Sentabr", "Oktyabr", "Noyabr", "Dekabr"};


    public BotState getAndCheck(Update update) {
        UserDTO userDTO = userService.checkAndGet(update);
        return userDTO.getState();
    }

    public Boolean getPhone(Update update) {
        return userService.getPhone(update);
    }

    public void saveData(Update update, UserDTO userDTO) {
        Message message = update.hasMessage() ? update.getMessage() : update.getCallbackQuery().getMessage();
        userService.saveData(message.getChatId().toString(), userDTO);
    }

    public void getMainMenuSend(BotState state, Update update) {
        Message message = update.hasMessage() ? update.getMessage() : update.getCallbackQuery().getMessage();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        if (!UserService.containsSpecialCharacters(message.getFrom().getFirstName())){
            sendName(update);
            userService.saveData(update.getMessage().getChatId().toString(), new UserDTO(BotState.SEND_PHONE));
        }else {
            if (!getPhone(update) && state == BotState.SEND_PHONE) {
                sendMessage.setText("Aссалому алайкум " + userService.getName(update) + "\n▪\uFE0FРўйхатдан ўтиш учун телефон рақамингизни юборинг!");
                sendMessage.setReplyMarkup(keyboard.createContactMarkup());
                feign.sendMessage(sendMessage);
            } else {
                getRole(update);
            }
        }
    }


    public void getRole(Update update){
        Long chatId = update.getMessage().getChatId();
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth("Админстратор", "2275157", StandardCharsets.UTF_8);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            Client client = new Client();
            client.setClientID(chatId.toString());
            HttpEntity<Client> entity = new HttpEntity<>(client, headers);

            ResponseEntity<byte[]> response = restTemplate
                    .setConnectTimeout(Duration.ofSeconds(15))
                    .setReadTimeout(Duration.ofSeconds(60))
                    .build()
                    .exchange(Constant.REQUEST_URI + "/bot/check", HttpMethod.POST, entity, byte[].class);
            System.out.println(response);
            byte[] responseBody = response.getBody();
            if (responseBody != null) {
                String text = new String(responseBody, StandardCharsets.UTF_8);
                if (text.equals("Employee")){
                    userService.saveRole(chatId.toString(), text);
                    SendMessage sendMessage = new SendMessage(chatId.toString(), "Сиз ходимлар рўйхатига қўшилгансиз\n\nКеракли маҳсулот коди ёки шртих кодини киритинг!");
                    sendMessage.setReplyMarkup(keyboard.nullBtn(chatId.toString()));
                    feign.sendMessage(sendMessage);
                }else if (text.equals("Contractor")){
                    userService.saveRole(chatId.toString(), text);
                    SendMessage sendMessage = new SendMessage(chatId.toString(), "Сиз контрагентлар рўйхатига қўшилгансиз!\n\nҚуйидаги буйруқлардан бирини танланг!");
                    sendMessage.setReplyMarkup(keyboard.panelBtns());
                    feign.sendMessage(sendMessage);
                }else if (text.equals("Nobody")){
                    SendMessage sendMessage = new SendMessage(chatId.toString(), "Рўйхатдан ўтиш муваффақиятли якунланди✅\n\n"+userService.getName(update)+", сиз ҳақингизда ҳеч қандай маълумот топилмади!\nМаълумотларингиз тасдиқланишини кутинг.");
                    sendMessage.setReplyMarkup(keyboard.startBtn());
                    feign.sendMessage(sendMessage);
                }else {
                    SendMessage sendMessage = new SendMessage(chatId.toString(), userService.getName(update)+", сиз ҳақингизда ҳеч қандай маълумот топилмади!\nМаълумотларингиз тасдиқланишини кутинг.");
                    feign.sendMessage(sendMessage);
                }
            } else {
                SendMessage sendMessage = new SendMessage(chatId.toString(), "Server bilan bog`liq xatolik bo`ldi! \n Internet sekin yoki ma'lumot hajmi juda yuqori! \n +998(90)309-66-33 bilan bog`laning.");
                feign.sendMessage(sendMessage);
            }
        }catch (Exception e){
            SendMessage sendMessage = new SendMessage(chatId.toString(), "Server bilan bog`liq xatolik bo`ldi! \n +998(90)309-66-33 bilan bog`laning.");
            feign.sendMessage(sendMessage);
        }
    }


    public void Employee(Update update){
        Long chatId = update.getMessage().getChatId();
        fileService.saveMethodType(chatId, "PRODUCT");
        fileService.saveCode(chatId, update.getMessage().getText());
        DateDTO dateDTO = fileService.getDateDto(chatId);
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth("Админстратор", "2275157", StandardCharsets.UTF_8);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            HttpEntity<DateDTO> entity = new HttpEntity<>(dateDTO, headers);

            ResponseEntity<byte[]> response = restTemplate
                    .setConnectTimeout(Duration.ofSeconds(15))
                    .setReadTimeout(Duration.ofSeconds(60))
                    .build()
                    .exchange(Constant.REQUEST_URI + "/bot/reports", HttpMethod.POST, entity, byte[].class);
            byte[] responseBody = response.getBody();
            if (responseBody != null) {
                String text = new String(responseBody, StandardCharsets.UTF_8);
                SendMessage sendMessage = new SendMessage(chatId.toString(), text);
                if (text.startsWith("Код") || text.startsWith("\nШтрих")){
                    productService.saveCode(update.getMessage().getText(), chatId.toString());
                }
                sendMessage.setReplyMarkup(keyboard.nullBtn(chatId.toString()));
                feign.sendMessage(sendMessage);
            } else {
                SendMessage sendMessage = new SendMessage(chatId.toString(), "Something went wrong! (Employee)");
                feign.sendMessage(sendMessage);
            }
        }catch (Exception e){
            SendMessage sendMessage = new SendMessage(chatId.toString(), "Ушбу маҳсулот ҳақида маълумот топилмади!");
            feign.sendMessage(sendMessage);
        }
    }

    public void sendMessageToUser(MessageDTO messageDTO, String username) {
        Users user = userService.getByChatId(messageDTO.getChatId());
        Market market = marketService.getMarketByUserName(username);
        balanceService.saveBalance(messageDTO, market, user);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(messageDTO.getChatId());
        sendMessage.setText("\uD83C\uDFEA " + market.getName() + "\n" + messageDTO.getText());
        feign.sendMessage(sendMessage);
    }

    public void getBalance(Update update) {
        Long chatId = update.getMessage().getChatId();
        String role = userService.getRole(chatId);
        if (Objects.nonNull(role)) {
            if (role.equals("Contractor")) {
                fileService.saveMethodType(chatId, "BALANCE");
            } else if (role.equals("Employee")) {
                fileService.saveMethodType(chatId, "EMPLOYEE");
            }
        DateDTO dateDTO = fileService.getDateDto(chatId);
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth("Админстратор", "2275157", StandardCharsets.UTF_8);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            HttpEntity<DateDTO> entity = new HttpEntity<>(dateDTO, headers);

            ResponseEntity<byte[]> response = restTemplate
                    .setConnectTimeout(Duration.ofSeconds(15))
                    .setReadTimeout(Duration.ofSeconds(60))
                    .build()
                    .exchange(Constant.REQUEST_URI + "/bot/reports", HttpMethod.POST, entity, byte[].class);
            byte[] responseBody = response.getBody();
            if (responseBody != null) {
                String text = new String(responseBody, StandardCharsets.UTF_8);
                SendMessage sendMessage = new SendMessage(chatId.toString(), text);
                feign.sendMessage(sendMessage);
            } else {
                SendMessage sendMessage = new SendMessage(chatId.toString(), "Сизда баланс бўйича ҳеч қандай маълумот мавжуд эмас!");
                feign.sendMessage(sendMessage);
            }
        }catch (Exception e){
            SendMessage sendMessage = new SendMessage(chatId.toString(), "Сизда баланс бўйича ҳеч қандай маълумот мавжуд эмас!");
            feign.sendMessage(sendMessage);
        }
        }else {
            SendMessage sendMessage = new SendMessage(chatId.toString(), "Буйруқдан фойдаланишга рухсат йўқ!");
            feign.sendMessage(sendMessage);
        }
    }

    public void getReport(BotState state, Update update) {
        try {
            Long chatId;
            if (update.hasCallbackQuery()) {
                chatId = update.getCallbackQuery().getMessage().getChatId();
            } else
                chatId = update.getMessage().getChatId();
            if (state == BotState.REPORTS) {
                fileService.saveEndDate(chatId, update.getCallbackQuery().getData());
                sendLoading(update);
                for (int i = 0; i <2; i++) {
                    sendReport(i, update);
                }
            } else {
                LocalDate[] monthDates = getFirstAndLastDayOfMonth(update.getCallbackQuery().getData());
                if (monthDates != null) {
                    LocalDate firstDay = monthDates[0];
                    LocalDate lastDay = monthDates[1];

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    fileService.saveStartDate(chatId, formatter.format(firstDay));
                    fileService.saveEndDate(chatId, formatter.format(lastDay));
                    sendLoading(update);
                    for (int i = 0; i <2; i++) {
                        sendReport(i, update);
                    }
                } else {
                    System.out.println("Oyni tanlashda xatolik yuz berdi");
                }
            }
        } catch (Exception e) {
            System.out.println(LocalDate.now()+" "+update.getMessage().getFrom().getFirstName()+", "+update.getMessage().getChatId()+", File jo`natish bilan bog`liq xatolik, botService.getReport");
        }
    }

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

    public void sendLoading(Update update){
        EditMessageText editMessageText = new EditMessageText();
        Long chatId;
        Integer messageId;
        if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
            messageId = update.getCallbackQuery().getMessage().getMessageId();
        } else {
            chatId = update.getMessage().getChatId();
            messageId = update.getMessage().getMessageId();
        }
        editMessageText.setChatId(chatId.toString());
        editMessageText.setMessageId(messageId);
        editMessageText.setText("⏳");
        feign.editMessageText(editMessageText);
    }
    public void sendStartDate(BotState state, Update update) {
        Long chatId = update.getMessage().getChatId();
        SendMessage sendMessage = new SendMessage();
        if (state == BotState.GET_START_DATEV2) {
            fileService.saveReportId(chatId, 2);
        }else if (state == BotState.GET_START_DATE)
            fileService.saveReportId(chatId, 1);
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText("⏰Бошланғич санани танланг:");
        sendMessage.setReplyMarkup(keyboard.calendarBtns(LocalDate.now().toString()));
        feign.sendMessage(sendMessage);
    }

    public void saveStartDate(BotState state, Update update) {
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        fileService.saveStartDate(chatId, update.getCallbackQuery().getData());
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId.toString());
        editMessageText.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
        String text = "⏰Бошланғич сана -> "+update.getCallbackQuery().getData()+"\n|\n" +
                "Охирги санани танланг:";
        editMessageText.setText(text);
        editMessageText.setReplyMarkup(keyboard.calendarBtns(LocalDate.now().toString()));
        feign.editMessageText(editMessageText);
    }

    public void editDate(Update update) {
        Long chatId = null;
        try {
            chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
            editMarkup.setChatId(chatId.toString());
            editMarkup.setMessageId(messageId);
            editMarkup.setReplyMarkup(null);
            editMarkup.setReplyMarkup(keyboard.calendarBtns(setDate(update).toString()));
            System.out.println(update.getCallbackQuery().getData());
            feign.editMessageReplyMarkup(editMarkup);
        } catch (Exception e) {
            assert chatId != null;
            SendMessage sendMessage = new SendMessage(chatId.toString(), "\uD83C\uDD98Давр танлашда хатолик юз берди! \n" +
                    "⬅\uFE0FЖараённи қайтадан бошланг");
            feign.sendMessage(sendMessage);
        }
    }

    public LocalDate setDate(Update update) {
        String inlineDate = update.getCallbackQuery().getData();
        String inD[] = inlineDate.split("-");
        int year = Integer.parseInt(inD[0]);
        int month = Integer.parseInt(inD[1]);
        int day = 01;
        return LocalDate.of(year, month, day);
    }

    public void sendReport(int i, Update update) {
        try {
            Long chatId;
            if (update.hasCallbackQuery()) {
                chatId = update.getCallbackQuery().getMessage().getChatId();
            } else {
                chatId = update.getMessage().getChatId();
            }
            String uri = "https://api.telegram.org/bot"+ Security.BOT_TOKEN + "/sendDocument";
            HttpPost httppost = new HttpPost(uri);
            if (i==0) {
                fileService.saveTypeFile(chatId, "xlsx");
            }else{
                fileService.saveTypeFile(chatId, "pdf");
            }
            DateDTO dateDTO = fileService.getDateDto(chatId);
            File reports = fileService.getReports(dateDTO);
            InputFile inputFile = new InputFile(reports);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("chat_id", chatId.toString());
            builder.addBinaryBody(inputFile.getMediaName(), inputFile.getNewMediaFile(),
                    ContentType.APPLICATION_OCTET_STREAM, inputFile.getMediaName());
            builder.addTextBody("document", inputFile.getAttachName());
            org.apache.http.HttpEntity multipart = builder.build();
            httppost.setEntity(multipart);
            CloseableHttpClient httpClient = HttpClients.createDefault();
            try {
                sendAllDate(update);
                httpClient.execute(httppost);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            sendClient404Error(update);
        }
    }

    private void sendClient404Error(Update update) {
        EditMessageText editMessageText = new EditMessageText();
        Long chatId;
        Integer messageId;
        if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId();
            messageId = update.getCallbackQuery().getMessage().getMessageId();
        } else {
            chatId = update.getMessage().getChatId();
            messageId = update.getMessage().getMessageId();
        }
        editMessageText.setChatId(chatId.toString());
        editMessageText.setMessageId(messageId);
        editMessageText.setText("\uD83D\uDCC3 Ҳали контрагентлар рўйхатига қўшилмагансиз!");
        feign.editMessageText(editMessageText);
    }

    public void sendAllDate(Update update){
        try {
            Long chatId;
            Integer messageId;
            if (update.hasCallbackQuery()) {
                chatId = update.getCallbackQuery().getMessage().getChatId();
                messageId = update.getCallbackQuery().getMessage().getMessageId();
            } else {
                chatId = update.getMessage().getChatId();
                messageId = update.getMessage().getMessageId();
            }
            DateDTO dateDTO = fileService.getDateDto(chatId);
            EditMessageText editMessageText = new EditMessageText();
            editMessageText.setChatId(chatId.toString());
            editMessageText.setMessageId(messageId);
            String text = "⏰ Бошланғич сана-> " + dateDTO.getStartDate() + "\n" + "⏰ Охирги сана -> " + dateDTO.getEndDate();
            editMessageText.setText(text);
            editMessageText.setReplyMarkup(null);
            feign.editMessageText(editMessageText);
        }catch (Exception e){
            System.out.println(LocalDate.now()+" "+update.getMessage().getFrom().getFirstName()+", "+update.getMessage().getChatId()+", Davr saqlashda xatolik yuz berdi, botService.sendAllDate");
        }
    }

    public static Month convertToMonthEnum(String monthName) {
        switch (monthName) {
            case "Yanvar":
                return Month.JANUARY;
            case "Fevral":
                return Month.FEBRUARY;
            case "Mart":
                return Month.MARCH;
            case "Aprel":
                return Month.APRIL;
            case "May":
                return Month.MAY;
            case "Iyun":
                return Month.JUNE;
            case "Iyul":
                return Month.JULY;
            case "Avgust":
                return Month.AUGUST;
            case "Sentabr":
                return Month.SEPTEMBER;
            case "Oktyabr":
                return Month.OCTOBER;
            case "Noyabr":
                return Month.NOVEMBER;
            case "Dekabr":
                return Month.DECEMBER;
            default:
                throw new IllegalArgumentException("Invalid month name: " + monthName);
        }
    }

    public void sendError(Update update) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setText("\uD83C\uDD98Давр танлашда хатолик юз берди! \n" +
                "⬅\uFE0FЖараённи қайтадан бошланг");
        editMessageText.setChatId(update.getCallbackQuery().getMessage().getChatId().toString());
        editMessageText.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
        editMessageText.setReplyMarkup(null);
        feign.editMessageText(editMessageText);
    }

    public void sendName(Update update) {
        Long chatId = update.getMessage().getChatId();
        SendMessage sendMessage = new SendMessage(chatId.toString(), "Илтимос, исмингизни юборинг\uD83D\uDC47");
        feign.sendMessage(sendMessage);
    }

    public void saveName(Update update) {
        userService.saveUserName(update);
        Message message = update.hasMessage() ? update.getMessage() : update.getCallbackQuery().getMessage();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId().toString());
        if (!getPhone(update)) {
            sendMessage.setText("Aссалому алайкум " + userService.getName(update) + "\n▪\uFE0FРўйхатдан ўтиш учун телефон рақамингизни юборинг!");
                sendMessage.setReplyMarkup(keyboard.createContactMarkup());
                feign.sendMessage(sendMessage);
            } else {
                getRole(update);
            }
        }
}
