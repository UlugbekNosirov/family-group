package uz.dataFin.notificationbot.service;

import com.spire.pdf.PdfDocument;
import com.spire.pdf.graphics.PdfImageType;
import lombok.RequiredArgsConstructor;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.tomcat.jni.Local;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.feign.TelegramFeign;
import uz.dataFin.notificationbot.helper.Keyboard;
import uz.dataFin.notificationbot.model.*;
import uz.dataFin.notificationbot.payload.UserDTO;
import uz.dataFin.notificationbot.utils.BotState;
import uz.dataFin.notificationbot.utils.Constant;
import uz.dataFin.notificationbot.utils.Security;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private final UtilService utilService;
    private final SalesReceiptService salesReceiptService;


    public BotState getAndCheck(Update update) {
        UserDTO userDTO = userService.checkAndGet(update);
//        sendFileToUser();
        return userDTO.getState();
    }

    public void saveData(Update update, UserDTO userDTO) {
        String chatId = utilService.getChatIdFromUpdate(update);
        userService.saveData(chatId, userDTO);
    }

    public void getMainMenuSend(Update update) {
        SendMessage sendMessage = new SendMessage();
        String chatId = utilService.getChatIdFromUpdate(update);
        sendMessage.setChatId(chatId);
        String name = userService.getName(update);
        if (UtilService.containsSpecialCharacters(name)) {
            sendName(update);
        }else if (!getPhone(update)) {
            sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.REGISTRATION));
            sendMessage.setReplyMarkup(keyboard.createContactMarkup());
            feign.sendMessage(sendMessage);
        }else {
            getRole(update);
        }
    }

    public void sendName(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        SendMessage sendMessage = new SendMessage(chatId, "Илтимос, исмингизни юборинг...");
        feign.sendMessage(sendMessage);
    }

    public Boolean getPhone(Update update) {
        return userService.getPhone(update);
    }

    public void getRole(Update update){
        String chatId = utilService.getChatIdFromUpdate(update);
        String role = userService.getRole(chatId);
        switch (role) {
            case "Employee" -> {
                SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.EMPLOYEE));
                sendMessage.setReplyMarkup(keyboard.nullBtn(chatId, chatId));
                feign.sendMessage(sendMessage);
            }
            case "Contractor" -> {
                SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CONTRACTOR));
                sendMessage.setReplyMarkup(keyboard.panelBtns(chatId));
                feign.sendMessage(sendMessage);
            }
            case "Nobody" -> {
                SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.ALREADY_REGISTRATION));
                sendMessage.setReplyMarkup(keyboard.startBtn());
                feign.sendMessage(sendMessage);
            }
            case "null" -> {
                SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_INFO_ROLE));
                feign.sendMessage(sendMessage);
            }
        }
    }


    public void Employee(Update update){
        String chatId = utilService.getChatIdFromUpdate(update);
        String role = userService.getRole(chatId);
        if (role.equals("Nobody") || role.equals("Contractor")){
            SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_PERMISSION));
            feign.sendMessage(sendMessage);
            return;
        }
        fileService.saveMethodType(chatId, "PRODUCT");
        fileService.saveCode(chatId, update.getMessage().getText());
        DateDTO dateDTO = fileService.getDateDto(chatId);
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth("Админстратор", "2275157", StandardCharsets.UTF_8);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<DateDTO> entity = new HttpEntity<>(dateDTO, headers);

            ResponseEntity<byte[]> response = restTemplate
                    .setConnectTimeout(Duration.ofSeconds(15))
                    .setReadTimeout(Duration.ofSeconds(60))
                    .build()
                    .exchange(Constant.REQUEST_URI + "/bot/reports", HttpMethod.POST, entity, byte[].class);
            byte[] responseBody = response.getBody();
            if (responseBody != null) {
                String text = new String(responseBody, StandardCharsets.UTF_8);
                SendMessage sendMessage = new SendMessage(chatId, text);
                if (text.startsWith("Код") || text.startsWith("\nШтрих")){
                    productService.saveCode(update.getMessage().getText(), chatId);
                }
                sendMessage.setReplyMarkup(keyboard.nullBtn(chatId, chatId));
                feign.sendMessage(sendMessage);
            } else {
                SendMessage sendMessage = new SendMessage(chatId, "Something went wrong! (Employee)");
                feign.sendMessage(sendMessage);
            }
        }catch (Exception e){
            SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.PRODUCT404));
            feign.sendMessage(sendMessage);
        }
    }

    public void sendMessageToUser(MessageDTO messageDTO, String username)  {
        Users user = userService.getByChatId(messageDTO.getChatId());
        Market market = marketService.getMarketByUserName(username);
        balanceService.saveBalance(messageDTO, market, user);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(messageDTO.getChatId());
        sendMessage.setText("\uD83C\uDFEA " + market.getName() + "\n" + messageDTO.getText());
        feign.sendMessage(sendMessage);
        if (Objects.nonNull(messageDTO.getUrl())){
            salesReceiptService.saveUrlById(messageDTO);
        }
    }

    public void sendFileToUser() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            List<SalesReceipt> nonActiveUsers = salesReceiptService.getNonActiveUsers();
            for (SalesReceipt salesReceipt : nonActiveUsers) {
                sendCheque(salesReceipt);
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    public void getBalance(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        String role = userService.getRole(chatId);
        if (role.equals("Contractor")) {
            fileService.saveMethodType(chatId, "BALANCE");
        } else if (role.equals("Employee")) {
            fileService.saveMethodType(chatId, "EMPLOYEE");
        } else {
            SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_PERMISSION));
            feign.sendMessage(sendMessage);
            return;
        }
        DateDTO dateDTO = fileService.getDateDto(chatId);
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth("Админстратор", "2275157", StandardCharsets.UTF_8);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<DateDTO> entity = new HttpEntity<>(dateDTO, headers);

            ResponseEntity<byte[]> response = restTemplate
                    .setConnectTimeout(Duration.ofSeconds(15))
                    .setReadTimeout(Duration.ofSeconds(60))
                    .build()
                    .exchange(Constant.REQUEST_URI + "/bot/reports", HttpMethod.POST, entity, byte[].class);
            byte[] responseBody = response.getBody();
            if (responseBody != null) {
                String text = new String(responseBody, StandardCharsets.UTF_8);
                SendMessage sendMessage = new SendMessage(chatId, text);
                feign.sendMessage(sendMessage);
            } else {
                SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_BALANCE));
                feign.sendMessage(sendMessage);
            }
        }catch (Exception e){
            SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_BALANCE));
            feign.sendMessage(sendMessage);
        }
    }

    public void getReport(BotState state, Update update) {
        try {
            String chatId = utilService.getChatIdFromUpdate(update);
            String role = userService.getRole(chatId);
            if (role.equals("Nobody") || role.equals("Employee")){
                SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_PERMISSION));
                feign.sendMessage(sendMessage);
                return;
            }
            if (state == BotState.REPORTS) {
                fileService.saveEndDate(chatId, update.getCallbackQuery().getData());
            } else if (state == BotState.SEND_BY_MONTH){
                LocalDate[] monthDates = UtilService.getFirstAndLastDayOfMonth(update.getCallbackQuery().getData());
                assert monthDates != null;
                fileService.saveStartDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(monthDates[0]));
                fileService.saveEndDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(monthDates[1]));
            } else if (state == BotState.SEND_BY_DAY) {
                fileService.saveStartDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now()));
                fileService.saveEndDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now()));
            } else if (state == BotState.SEND_BY_WEEK) {
                LocalDate[] day = utilService.getBeginningOfWeekAndToday();
                fileService.saveStartDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(day[0]));
                fileService.saveEndDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(day[1]));
            } else if (state == BotState.SEND_BY_LAST_MONTH) {
                LocalDate[] dates = UtilService.getFirstDayOfMonthAndToday();
                fileService.saveStartDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(dates[0]));
                fileService.saveEndDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(dates[1]));
            } else if (state == BotState.SEND_BY_SEASON) {
                LocalDate[] firstDayOfQuarterAndToday = UtilService.getFirstDayOfQuarterAndToday();
                fileService.saveStartDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(firstDayOfQuarterAndToday[0]));
                fileService.saveEndDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(firstDayOfQuarterAndToday[1]));
            } else if (state == BotState.SEND_BY_YEAR) {
                LocalDate[] firstDayOfYearAndToday = UtilService.getFirstDayOfYearAndToday();
                fileService.saveStartDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(firstDayOfYearAndToday[0]));
                fileService.saveEndDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(firstDayOfYearAndToday[1]));
            }
            sendAllDate(update);
            sendReport(update);
        } catch (Exception e) {
            System.out.println(LocalDate.now()+" "+userService.getName(update)+", "+utilService.getChatIdFromUpdate(update)+", File jo`natish bilan bog`liq xatolik, botService.getReport");
        }
    }

    public void sendStartDate(BotState state, Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        String role = userService.getRole(chatId);
        if (role.equals("Nobody") || role.equals("Employee")){
            SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_PERMISSION));
            feign.sendMessage(sendMessage);
            return;
        }
        SendMessage sendMessage = new SendMessage();
        if (state == BotState.GET_START_DATEV2) {
            fileService.saveReportId(chatId, 2);
        }else if (state == BotState.GET_START_DATE)
            fileService.saveReportId(chatId, 1);
        sendMessage.setChatId(chatId);
        sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.CHOOSE_PERIOD));
        sendMessage.setReplyMarkup(keyboard.periodKeyboards(chatId));
        feign.sendMessage(sendMessage);
    }

    public void saveStartDate( Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        fileService.saveStartDate(chatId, update.getCallbackQuery().getData());
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
        String text = utilService.getTextByLanguage(chatId, Constant.START_DATE)+update.getCallbackQuery().getData()+"\n|\n" +
                utilService.getTextByLanguage(chatId, Constant.CHOOSE_END_DATE);
        editMessageText.setText(text);
        editMessageText.setReplyMarkup(keyboard.calendarBtns(LocalDate.now().toString()));
        feign.editMessageText(editMessageText);
    }

    public void editDate(Update update) {
        try {
            String chatId = utilService.getChatIdFromUpdate(update);
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
            editMarkup.setChatId(chatId);
            editMarkup.setMessageId(messageId);
            editMarkup.setReplyMarkup(null);
            editMarkup.setReplyMarkup(keyboard.calendarBtns(utilService.setDate(update).toString()));
            System.out.println(update.getCallbackQuery().getData());
            feign.editMessageReplyMarkup(editMarkup);
        } catch (Exception e) {
            String chatId = utilService.getChatIdFromUpdate(update);
            SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.ERROR_CHOOSE_DATE));
            feign.sendMessage(sendMessage);
        }
    }

    public void sendCheque(SalesReceipt salesReceipt) {

        File reports = fileService.getCheque(salesReceipt);
            try {
                PDF2IMAGEinCheque(reports.getAbsolutePath(), salesReceipt);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


    }

    public void sendReport(Update update) {
            try {
            String chatId = utilService.getChatIdFromUpdate(update);
            String uri = "https://api.telegram.org/bot" + Security.BOT_TOKEN + "/sendDocument";
            HttpPost httppost = new HttpPost(uri);
            DateDTO dateDto = fileService.getDateDto(chatId);
            if (dateDto.getTypeFile().equals("jpg")) {
                sendReportAsImage(update);
            } else {
                DateDTO dateDTO = fileService.getDateDto(chatId);
                File reports = fileService.getReports(dateDTO);
                InputFile inputFile = new InputFile(reports);
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.addTextBody("chat_id", chatId);
                builder.addBinaryBody(inputFile.getMediaName(), inputFile.getNewMediaFile(),
                        ContentType.APPLICATION_OCTET_STREAM, inputFile.getMediaName());
                builder.addTextBody("document", inputFile.getAttachName());
                org.apache.http.HttpEntity multipart = builder.build();
                httppost.setEntity(multipart);
                CloseableHttpClient httpClient = HttpClients.createDefault();
                try {
                    httpClient.execute(httppost);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            } catch(Exception e){
                sendClient404Error(update);
            }
    }

    public void sendReportAsImage(Update update) {
        try {
            String chatId = utilService.getChatIdFromUpdate(update);
            fileService.saveTypeFile(chatId, "pdf");
            DateDTO dateDTO = fileService.getDateDto(chatId);
            File reports = fileService.getReports(dateDTO);
            PDF2IMAGE(reports.getAbsolutePath(), update);
            fileService.saveTypeFile(chatId, "jpg");
        } catch (Exception e) {
            sendClient404Error(update);
        }
    }

    public void sendPhoto(Update update, File file) {
        try {
            String chatId = utilService.getChatIdFromUpdate(update);
            String uri = "https://api.telegram.org/bot" + Security.BOT_TOKEN + "/sendPhoto";
            HttpPost httppost = new HttpPost(uri);

            File photoFile = new File(file.getAbsolutePath());

            if (!photoFile.exists() || !photoFile.canRead()) {
                throw new RuntimeException("Photo file does not exist or is not readable");
            }

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("chat_id", chatId);

            builder.addBinaryBody("photo", photoFile, ContentType.APPLICATION_OCTET_STREAM, photoFile.getName());

            org.apache.http.HttpEntity multipart = builder.build();
            httppost.setEntity(multipart);

            CloseableHttpClient httpClient = HttpClients.createDefault();
            try {
                httpClient.execute(httppost);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            sendClient404Error(update);
        }
    }

    public void sendChequeAsPhoto(SalesReceipt salesReceipt, File file) {
        try {
            String chatId = salesReceipt.getClientId();
            String uri = "https://api.telegram.org/bot" + Security.BOT_TOKEN + "/sendPhoto";
            HttpPost httppost = new HttpPost(uri);

            File photoFile = new File(file.getAbsolutePath());

            if (!photoFile.exists() || !photoFile.canRead()) {
                throw new RuntimeException("Photo file does not exist or is not readable");
            }

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("chat_id", chatId);

            builder.addBinaryBody("photo", photoFile, ContentType.APPLICATION_OCTET_STREAM, photoFile.getName());

            org.apache.http.HttpEntity multipart = builder.build();
            httppost.setEntity(multipart);

            CloseableHttpClient httpClient = HttpClients.createDefault();
            try {
                httpClient.execute(httppost);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            System.out.println(e+"Xatolik botService().SendCheque");
        }
    }


    public void PDF2IMAGE(String filePath, Update update) throws IOException {
        PdfDocument pdf = new PdfDocument();
        pdf.loadFromFile(filePath);
        for (int i = 0; i < pdf.getPages().getCount(); i++) {
            BufferedImage image = pdf.saveAsImage(i, PdfImageType.Bitmap,500,500);
            Path path= Paths.get("IMAGE");
            path=utilService.checkPackage(path);
            File file = new File(path.toFile().getAbsolutePath() + "/" + String.format(("report-%d.png"), i));
            ImageIO.write(image, "PNG", file);
            sendPhoto(update, file);
        }
        pdf.close();
    }

    public void PDF2IMAGEinCheque(String filePath, SalesReceipt salesReceipt) throws IOException {
        PdfDocument pdf = new PdfDocument();
        pdf.loadFromFile(filePath);
        for (int i = 0; i < pdf.getPages().getCount(); i++) {
            BufferedImage image = pdf.saveAsImage(i, PdfImageType.Bitmap,500,500);
            Path path= Paths.get("CHEQUE");
            path=utilService.checkPackage(path);
            File file = new File(path.toFile().getAbsolutePath() + "/" + String.format(("cheque-%d.png"), i));
            ImageIO.write(image, "PNG", file);
            sendChequeAsPhoto(salesReceipt, file);
        }
        pdf.close();
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
        editMessageText.setText(utilService.getTextByLanguage(chatId.toString(), Constant.ERROR_500));
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
            DateDTO dateDTO = fileService.getDateDto(chatId.toString());
            EditMessageText editMessageText = new EditMessageText();
            editMessageText.setChatId(chatId.toString());
            editMessageText.setMessageId(messageId);
            String text = utilService.getTextByLanguage(chatId.toString(), Constant.START_DATE) + dateDTO.getStartDate() + "\n" + utilService.getTextByLanguage(chatId.toString(), Constant.END_DATE) + dateDTO.getEndDate();
            editMessageText.setText(text);
            editMessageText.setReplyMarkup(null);
            feign.editMessageText(editMessageText);
        }catch (Exception e){
            System.out.println(LocalDate.now()+" "+update.getMessage().getFrom().getFirstName()+", "+update.getMessage().getChatId()+", Davr saqlashda xatolik yuz berdi, botService.sendAllDate");
        }
    }

    public void sendError(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setText(utilService.getTextByLanguage(chatId, Constant.ERROR_CHOOSE_DATE));
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
        editMessageText.setReplyMarkup(null);
        feign.editMessageText(editMessageText);
    }


    public void saveName(Update update) {
        String name = userService.getName(update);
        SendMessage sendMessage = new SendMessage();
        String chatId = utilService.getChatIdFromUpdate(update);
        Message message = utilService.getMessageFromUpdate(update);
        sendMessage.setChatId(chatId);
        if (UtilService.containsSpecialCharacters(name)) {
            userService.saveUserName(update);
        }
        if (!getPhone(update)) {
            sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.REGISTRATION));
            feign.sendMessage(sendMessage);
        }
        if (message.getText().equals(utilService.getTextByLanguage(chatId, Constant.AKT_SVERKA))){
            sendStartDate(BotState.GET_START_DATEV2, update);
            return;
        }
        if (message.getText().equals(utilService.getTextByLanguage(chatId, Constant.BALANCE))){
            getBalance(update);
            return;
        }
        if (message.getText().equals(utilService.getTextByLanguage(chatId, Constant.AKT_SVERKA_TOVAR))){
            sendStartDate(BotState.GET_START_DATE, update);
            return;
        }
//        if (message.getText().equals("/settings")){
//            sendTypeFile(update);
//            return;
//        }
        if (UtilService.containsOnlyNumbers(update.getMessage().getText())){
            Employee(update);
        }
    }

    public void sendSettings(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.MAIN_MENU));
        sendMessage.setReplyMarkup(keyboard.settings(chatId));
        feign.sendMessage(sendMessage);
    }

    public void editTypeFile(BotState state, Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        String role = userService.getRole(chatId);
        if (role.equals("Nobody")){
            SendMessage sendMessage = new SendMessage();
            sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.NO_PERMISSION));
            feign.sendMessage(sendMessage);
            return;
        }
        Message message = (update.hasMessage())?update.getMessage():update.getCallbackQuery().getMessage();
        if (state == BotState.EDIT2XLSX){
            fileService.saveTypeFile(chatId, "xlsx");
        } else if (state == BotState.EDIT2PDF) {
            fileService.saveTypeFile(chatId, "pdf");
        } else if (state == BotState.EDIT2JPG) {
            fileService.saveTypeFile(chatId, "jpg");
        }
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.setText(utilService.getTextByLanguage(chatId, Constant.CHANGE_TYPE_FILE_200)+update.getCallbackQuery().getData());
        feign.editMessageText(editMessageText);
    }

    public void sendStartDateAsCalendar(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        Message message = utilService.getMessageFromUpdate(update);
        String role = userService.getRole(chatId);
        if (role.equals("Nobody") || role.equals("Employee")){
            SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_PERMISSION));
            feign.sendMessage(sendMessage);
            return;
        }
        EditMessageText sendMessage = new EditMessageText();
        sendMessage.setChatId(chatId);
        sendMessage.setMessageId(message.getMessageId());
        sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.CHOOSE_START_DATE));
        sendMessage.setReplyMarkup(keyboard.calendarBtns(LocalDate.now().toString()));
        feign.editMessageText(sendMessage);
    }

    public void sendTypeFile(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.CHOOSE_FORMAT));
        sendMessage.setReplyMarkup(keyboard.typeFileButtons(chatId));
        feign.sendMessage(sendMessage);
    }

    public void sendLanguage(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.CHOOSE_LANGUAGE));
        sendMessage.setReplyMarkup(keyboard.chooseLanguage());
        feign.sendMessage(sendMessage);
    }

    public void editLanguage(BotState state, Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        String role = userService.getRole(chatId);
        SendMessage sendMessage = new SendMessage();
        if (role.equals("Nobody")){
            sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.NO_PERMISSION));
            feign.sendMessage(sendMessage);
            return;
        }
        Message message = (update.hasMessage())?update.getMessage():update.getCallbackQuery().getMessage();
        if (state == BotState.EDIT2UZ){
            userService.saveLanguage(chatId, "uz");
        } else if (state == BotState.EDIT2RU) {
            userService.saveLanguage(chatId, "ru");
        } else if (state == BotState.EDIT2KRIL) {
            userService.saveLanguage(chatId, "kril");
        }
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(message.getMessageId());
        editMessageText.setText(utilService.getTextByLanguage(chatId, Constant.CHANGE_LANGUAGE_200)+update.getCallbackQuery().getData());
        feign.editMessageText(editMessageText);
        sendMessage.setChatId(chatId);
        sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.TO_BE_CONTINUED));
        sendMessage.setReplyMarkup(keyboard.settings(chatId));
        feign.sendMessage(sendMessage);
    }

    public void sendMainMenu(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.MAIN_MENU));
        sendMessage.setReplyMarkup(keyboard.panelBtns(chatId));
        feign.sendMessage(sendMessage);
    }

    public void sendContinue(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.MAIN_MENU));
        feign.sendMessage(sendMessage);
    }
}
