package uz.dataFin.notificationbot.service;

import com.spire.pdf.PdfDocument;
import com.spire.pdf.graphics.PdfImageType;
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
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
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
import javax.print.CancelablePrintJob;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
    public final ReportService reportService;
    public final UpdateUserStatusService updateUserStatusService;
    public final ProductService productService;
    private final Keyboard keyboard;
    private final UtilService utilService;
    private final SalesReceiptService salesReceiptService;


    public BotState getAndCheck(Update update) {
        UserDTO userDTO = userService.checkAndGet(update);
        return userDTO.getState();
    }

    public void saveData(Update update, UserDTO userDTO) {
        String chatId = utilService.getChatIdFromUpdate(update);
        userService.saveData(chatId, userDTO);
    }

    public void getMainMenuSend(String chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        String name = userService.getName(chatId);
        if (UtilService.containsSpecialCharacters(name)) {
            sendName(chatId);
        }else if (!getPhone(chatId)) {
            sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.REGISTRATION));
            sendMessage.setReplyMarkup(keyboard.createContactMarkup());
            feign.sendMessage(sendMessage);
        }else {
            getRole(chatId);
        }
    }

    public void sendName(String chatId) {
        SendMessage sendMessage = new SendMessage(chatId, "Илтимос, исмингизни юборинг...");
        feign.sendMessage(sendMessage);
    }

    public Boolean getPhone(String chatID) {
        return userService.getPhone(chatID);
    }

    public void getRole(String chatId){
        String role = userService.getRole(chatId);
        switch (role) {
            case "Employee" -> {
                SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.EMPLOYEE));
                sendMessage.setReplyMarkup(keyboard.nullBtn(chatId, chatId));
                feign.sendMessage(sendMessage);
            }
            case "Contractor" -> {
                SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CONTRACTOR));
                sendMessage.setReplyMarkup(keyboard.panelBtns(role, chatId));
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


    public void Employee(Message message){
        String chatId = message.getChatId().toString();
        String role = userService.getRole(chatId);
        if (role.equals("Nobody") || role.equals("Contractor")){
            SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_PERMISSION));
            feign.sendMessage(sendMessage);
            return;
        }
        fileService.saveMethodType(chatId, "PRODUCT");
        fileService.saveCode(chatId, message.getText());
        DateDTO dateDTO = fileService.getDateDto(chatId);
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(Security.LOGIN, Security.PASSWORD, StandardCharsets.UTF_8);
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
                    productService.saveCode(message.getText(), chatId);
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
        if (Objects.nonNull(messageDTO.getDocumentID())){
            sendMessage.setReplyMarkup(keyboard.Query(messageDTO.getChatId()));
            UpdateUserStatus updateUserStatus = UpdateUserStatus.builder()
                    .chatId(messageDTO.getChatId())
                    .Text("\uD83C\uDFEA " + market.getName() + "\n" + messageDTO.getText())
                    .documentID(messageDTO.getDocumentID())
                    .clientId(messageDTO.getClientId())
                    .typeDocument(messageDTO.getTypeDocument())
                    .status("rejected").build();
            updateUserStatusService.saveData(updateUserStatus);
        }
        feign.sendMessage(sendMessage);
    }

//    public void sendFileToUser() {
//        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//        scheduler.scheduleAtFixedRate(() -> {
//            List<SalesReceipt> nonActiveUsers = salesReceiptService.getNonActiveUsers();
//            for (SalesReceipt salesReceipt : nonActiveUsers) {
//                sendCheque(salesReceipt);
//            }
//        }, 0, 1, TimeUnit.MINUTES);
//    }

    public void getBalance(String chatId) {
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
            headers.setBasicAuth(Security.LOGIN, Security.PASSWORD, StandardCharsets.UTF_8);
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

    public void getReport(BotState state, String chatId, Integer messageId, String callBackData ) {
        try {
            String role = userService.getRole(chatId);
            LocalDate[] dates = new LocalDate[]{LocalDate.now(), LocalDate.now()};
            if (role.equals("Nobody") || role.equals("Employee")) {
                SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_PERMISSION));
                feign.sendMessage(sendMessage);
                return;
            }

            if (state == BotState.SEND_BY_MONTH) {
                dates = UtilService.getFirstAndLastDayOfMonth(callBackData);
                assert dates != null;
            } else if (state == BotState.SEND_BY_WEEK) {
                dates = utilService.getBeginningOfWeekAndToday();
            } else if (state == BotState.SEND_BY_LAST_MONTH) {
                dates = UtilService.getFirstDayOfMonthAndToday();
            } else if (state == BotState.SEND_BY_SEASON) {
                dates = UtilService.getFirstDayOfQuarterAndToday();
            } else if (state == BotState.SEND_BY_YEAR) {
                dates = UtilService.getFirstDayOfYearAndToday();
            }
            if (state == BotState.REPORTS) {
                fileService.saveEndDate(chatId, callBackData);
            } else {
                fileService.saveStartDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(dates[0]));
                fileService.saveEndDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(dates[1]));
            }
            if (role.equals("Contractor")) {
                sendAllDate(chatId, messageId);
                sendReport(chatId, messageId);
            } else if (role.equals("ADMIN")) {
                if (state == BotState.REPORTS) {
                    reportService.saveEndDate(chatId, callBackData);
                } else {
                    reportService.saveStartDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(dates[0]));
                    reportService.saveEndDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(dates[1]));
                }
                sendWareHouseButton(chatId, messageId);
            }
        } catch (Exception e) {
            System.out.println(LocalDate.now()+" "+userService.getName(chatId)+", "+chatId+", File jo`natish bilan bog`liq xatolik, botService.getReport");
        }
    }

    private void sendWareHouseButton(String chatId, Integer messageId) {
        try {
            ReportDTO reportDto = reportService.getReportDto(chatId);
            EditMessageText editMessageText = new EditMessageText();
            editMessageText.setChatId(chatId);
            editMessageText.setMessageId(messageId);
            String text = utilService.getTextByLanguage(chatId, Constant.START_DATE) + reportDto.getStartDate() + "\n" + utilService.getTextByLanguage(chatId, Constant.END_DATE) + reportDto.getEndDate();
            editMessageText.setText(text);
            editMessageText.setReplyMarkup(null);
            SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CHOOSE_WAREHOUSE));
            sendMessage.setReplyMarkup(keyboard.wareHouse(chatId));
            feign.editMessageText(editMessageText);
        }catch (Exception e){
            System.out.println(LocalDate.now()+", "+chatId+", Davr saqlashda xatolik yuz berdi, botService.sendAllDate");
        }
    }

    public void sendStartDate(BotState state, String chatId) {
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
        else if (state == BotState.GET_START_DATE_WAREHOUSE)
            reportService.saveNew(chatId);
        sendMessage.setChatId(chatId);
        sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.CHOOSE_PERIOD));
        sendMessage.setReplyMarkup(keyboard.periodKeyboards(chatId));
        feign.sendMessage(sendMessage);
    }

    public void saveStartDate(Message message, String chatId, String callbackQuery) {
        fileService.saveStartDate(chatId, callbackQuery);
        reportService.saveStartDate(chatId, callbackQuery);
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(message.getMessageId());
        String text = utilService.getTextByLanguage(chatId, Constant.START_DATE)+callbackQuery+"\n|\n" +
                utilService.getTextByLanguage(chatId, Constant.CHOOSE_END_DATE);
        editMessageText.setText(text);
        editMessageText.setReplyMarkup(keyboard.calendarBtns(LocalDate.now().toString()));
        feign.editMessageText(editMessageText);
    }

    public void editDate(String chatId, Integer messageId, String callBackData) {
        try {
            EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
            editMarkup.setChatId(chatId);
            editMarkup.setMessageId(messageId);
            editMarkup.setReplyMarkup(null);
            editMarkup.setReplyMarkup(keyboard.calendarBtns(utilService.setDate(callBackData).toString()));
            System.out.println(callBackData);
            feign.editMessageReplyMarkup(editMarkup);
        } catch (Exception e) {
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

    public void sendReport(String chatId, Integer messageId) {
            try {
            String uri = "https://api.telegram.org/bot" + Security.BOT_TOKEN + "/sendDocument";
            HttpPost httppost = new HttpPost(uri);
            DateDTO dateDto = fileService.getDateDto(chatId);
            if (dateDto.getTypeFile().equals("jpg")) {
                sendReportAsImage(chatId, messageId);
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
                sendClient404Error(chatId, messageId);
            }
    }

    public void sendReportAsImage(String chatId, Integer messageId) {
        try {
            fileService.saveTypeFile(chatId, "pdf");
            DateDTO dateDTO = fileService.getDateDto(chatId);
            File reports = fileService.getReports(dateDTO);
            PDF2IMAGE(reports.getAbsolutePath(), messageId, chatId);
            fileService.saveTypeFile(chatId, "jpg");
        } catch (Exception e) {
            sendClient404Error(chatId, messageId);
        }
    }

    public void sendPhoto(Integer messageId, File file, String chatId) {
        try {
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
            sendClient404Error(chatId, messageId);
        }
    }

    public void sendChequeAsPhoto(SalesReceipt salesReceipt, File file) {
        try {
            String chatId = salesReceipt.getClientId();
            String uri = Security.REQUEST_FULL_PATH + "/sendPhoto";
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


    public void PDF2IMAGE(String filePath, Integer messageId, String chatId) throws IOException {
        PdfDocument pdf = new PdfDocument();
        pdf.loadFromFile(filePath);
        for (int i = 0; i < pdf.getPages().getCount(); i++) {
            BufferedImage image = pdf.saveAsImage(i, PdfImageType.Bitmap,500,500);
            Path path= Paths.get("IMAGE");
            path=utilService.checkPackage(path);
            File file = new File(path.toFile().getAbsolutePath() + "/" + String.format(("report-%d.png"), i));
            ImageIO.write(image, "PNG", file);
            sendPhoto(messageId, file, chatId);
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

    private void sendClient404Error(String chatId, Integer messageId) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId);
        editMessageText.setText(utilService.getTextByLanguage(chatId, Constant.ERROR_500));
        feign.editMessageText(editMessageText);
    }


    public void sendAllDate(String chatId, Integer messageId){
        try {
            DateDTO dateDTO = fileService.getDateDto(chatId);
            EditMessageText editMessageText = new EditMessageText();
            editMessageText.setChatId(chatId);
            editMessageText.setMessageId(messageId);
            String text = utilService.getTextByLanguage(chatId, Constant.START_DATE) + dateDTO.getStartDate() + "\n" + utilService.getTextByLanguage(chatId, Constant.END_DATE) + dateDTO.getEndDate();
            editMessageText.setText(text);
            editMessageText.setReplyMarkup(null);
            feign.editMessageText(editMessageText);
        }catch (Exception e){
            System.out.println(LocalDate.now()+", "+chatId+", Davr saqlashda xatolik yuz berdi, botService.sendAllDate");
        }
    }

    public void sendError(String chatId, Integer messageId) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setText(utilService.getTextByLanguage(chatId, Constant.ERROR_CHOOSE_DATE));
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId);
        editMessageText.setReplyMarkup(null);
        feign.editMessageText(editMessageText);
    }


    public void saveName(Message message, String chatId) {
        String name = userService.getName(chatId);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        if (UtilService.containsSpecialCharacters(name)) {
            userService.saveUserName(message, chatId);
        }
        if (!getPhone(chatId)) {
            sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.REGISTRATION));
            sendMessage.setReplyMarkup(keyboard.createContactMarkup());
            feign.sendMessage(sendMessage);
        }
        if (message.getText().equals(utilService.getTextByLanguage(chatId, Constant.AKT_SVERKA))){
            sendStartDate(BotState.GET_START_DATEV2, chatId);
            return;
        }
        if (message.getText().equals(utilService.getTextByLanguage(chatId, Constant.BALANCE))){
            getBalance(chatId);
            return;
        }
        if (message.getText().equals(utilService.getTextByLanguage(chatId, Constant.AKT_SVERKA_TOVAR))){
            sendStartDate(BotState.GET_START_DATE, chatId);
            return;
        }
        if (UtilService.containsOnlyNumbers(message.getText())){
            Employee(message);
        }
    }

    public void sendSettings(String chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.MAIN_MENU));
        sendMessage.setReplyMarkup(keyboard.settings(chatId));
        feign.sendMessage(sendMessage);
    }

    public void editTypeFile(BotState state, String chatId, Integer messageId, String callBackData) {
        String role = userService.getRole(chatId);
        if (role.equals("Nobody")){
            SendMessage sendMessage = new SendMessage();
            sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.NO_PERMISSION));
            feign.sendMessage(sendMessage);
            return;
        }
        if (state == BotState.EDIT2XLSX){
            fileService.saveTypeFile(chatId, "xlsx");
        } else if (state == BotState.EDIT2PDF) {
            fileService.saveTypeFile(chatId, "pdf");
        } else if (state == BotState.EDIT2JPG) {
            fileService.saveTypeFile(chatId, "jpg");
        }
        try {
            EditMessageText editMessageText = new EditMessageText();
            editMessageText.setChatId(chatId);
            editMessageText.setMessageId(messageId);
            editMessageText.setText(utilService.getTextByLanguage(chatId, Constant.CHANGE_TYPE_FILE_200) + callBackData);
            feign.editMessageText(editMessageText);
        }catch (Exception e){
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.CHANGE_TYPE_FILE_200) + callBackData);
            feign.sendMessage(sendMessage);
        }
        }


    public void sendStartDateAsCalendar(Message message, String chatId) {
        String role = userService.getRole(chatId);
        if (role.equals("Nobody") || role.equals("Employee")) {
            SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_PERMISSION));
            feign.sendMessage(sendMessage);
            return;
        }
        try {
            EditMessageText sendMessage = new EditMessageText();
            sendMessage.setChatId(chatId);
            sendMessage.setMessageId(message.getMessageId());
            sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.CHOOSE_START_DATE));
            sendMessage.setReplyMarkup(keyboard.calendarBtns(LocalDate.now().toString()));
            feign.editMessageText(sendMessage);
        }catch (Exception e){
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.CHOOSE_START_DATE));
            sendMessage.setReplyMarkup(keyboard.calendarBtns(LocalDate.now().toString()));
            feign.sendMessage(sendMessage);
        }
    }

    public void sendTypeFile(String chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.CHOOSE_FORMAT));
        sendMessage.setReplyMarkup(keyboard.typeFileButtons(chatId));
        feign.sendMessage(sendMessage);
    }

    public void sendLanguage(String chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.CHOOSE_LANGUAGE));
        sendMessage.setReplyMarkup(keyboard.chooseLanguage());
        feign.sendMessage(sendMessage);
    }

    public void editLanguage(BotState state, String chatId, Integer messageId, String callBackData) {
        String role = userService.getRole(chatId);
        SendMessage sendMessage = new SendMessage();
        if (role.equals("Nobody")){
            sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.NO_PERMISSION));
            feign.sendMessage(sendMessage);
            return;
        }
        if (state == BotState.EDIT2UZ){
            userService.saveLanguage(chatId, "uz");
        } else if (state == BotState.EDIT2RU) {
            userService.saveLanguage(chatId, "ru");
        } else if (state == BotState.EDIT2KRIL) {
            userService.saveLanguage(chatId, "kril");
        }
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId);
        editMessageText.setText(utilService.getTextByLanguage(chatId, Constant.CHANGE_LANGUAGE_200)+callBackData);
        feign.editMessageText(editMessageText);
        sendMessage.setChatId(chatId);
        sendMessage.setText(utilService.getTextByLanguage(chatId, Constant.TO_BE_CONTINUED));
        sendMessage.setReplyMarkup(keyboard.settings(chatId));
        feign.sendMessage(sendMessage);
    }

    public void sendMainMenu(String chatId) {
        SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.MAIN_MENU));
        sendMessage.setReplyMarkup(keyboard.panelBtns(userService.getRole(chatId), chatId));
        feign.sendMessage(sendMessage);
    }

    public void sendContinue(String chatId) {
        SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.MAIN_MENU));
        feign.sendMessage(sendMessage);
    }

    public void sendCheckingMoney(BotState state, Message message) {
        UpdateUserStatus data = updateUserStatusService.getData(message.getChatId().toString(), message.getText());
        if (state == BotState.SEND_AGREED_MONEY) {
            Integer response = postStatus(data, "agreed");
            if (response == 200) {
                try {
                    EditMessageText editMessageText = new EditMessageText();
                    editMessageText.setChatId(message.getChatId().toString());
                    editMessageText.setReplyMarkup(null);
                    editMessageText.setMessageId(message.getMessageId());
                    editMessageText.setText(utilService.getTextByLanguage(message.getChatId().toString(), Constant.ACCEPT));
                    feign.editMessageText(editMessageText);
                } catch (Exception e) {
                    System.out.println("Cannot edit message text yes/no");
                }
            }
        } else if (state == BotState.SEND_REJECTED_MONEY) {
            Integer response = postStatus(data, "rejected");
            if (response == 200) {
                try {
                    EditMessageText editMessageText = new EditMessageText();
                    editMessageText.setChatId(message.getChatId().toString());
                    editMessageText.setReplyMarkup(null);
                    editMessageText.setMessageId(message.getMessageId());
                    editMessageText.setText(utilService.getTextByLanguage(message.getChatId().toString(), Constant.REJECT));
                    feign.editMessageText(editMessageText);
                } catch (Exception e) {
                    System.out.println("Cannot edit message text yes/no");
                }
            }
        }
    }

    public Integer postStatus(UpdateUserStatus data, String status) {
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(Security.LOGIN, Security.PASSWORD, StandardCharsets.UTF_8);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            data.setStatus(status);
            UpdateUserStatus updateUserStatus = updateUserStatusService.updateStatus(data, status);
            HttpEntity<UpdateUserStatus> entity = new HttpEntity<>(updateUserStatus, headers);

            ResponseEntity<byte[]> response = restTemplate
                    .setConnectTimeout(Duration.ofSeconds(60))
                    .setReadTimeout(Duration.ofSeconds(60))
                    .build()
                    .exchange(Constant.REQUEST_URI +"/bot/update/status", HttpMethod.POST, entity, byte[].class);

            return response.getStatusCodeValue();
        }catch (Exception e){
            System.out.println(LocalDate.now()+" "+data.getText()+", "+", problem");
        }
        return 200;
    }
}
