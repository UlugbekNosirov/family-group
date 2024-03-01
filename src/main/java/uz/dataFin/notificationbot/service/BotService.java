package uz.dataFin.notificationbot.service;

import com.spire.pdf.PdfDocument;
import com.spire.pdf.graphics.PdfImageType;
import lombok.RequiredArgsConstructor;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.checkerframework.checker.units.qual.C;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResult;
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import uz.dataFin.notificationbot.feign.TelegramFeign;
import uz.dataFin.notificationbot.helper.Keyboard;
import uz.dataFin.notificationbot.model.*;
import uz.dataFin.notificationbot.payload.UserDTO;
import uz.dataFin.notificationbot.repository.SearchRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
    public final SearchRepository searchRepository;
    public final UpdateUserStatusService updateUserStatusService;
    public final ProductService productService;
    private final Keyboard keyboard;
    private final Api1CService api1CService;
    private final UtilService utilService;
    private final WarehouseService warehouseService;


    public BotState getAndCheck(Update update) {
        UserDTO userDTO = userService.checkAndGet(update);
        return userDTO.getState();
    }

    public void saveData(Update update, UserDTO userDTO) {
        String chatId = utilService.getChatIdFromUpdate(update);
        userService.saveData(chatId, userDTO);
    }

    public void getMainMenuSend(String chatId) {
        String name = userService.getName(chatId);
        if (utilService.containsSpecialCharacters(name)) {
            utilService.sendMessage(chatId, "Илтимос, исмингизни юборинг...", null, null);
        } else if (!getPhone(chatId)) {
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.REGISTRATION), keyboard.createContactMarkup(chatId), null);
        } else {
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

    public void getRole(String chatId) {
        api1CService.saveClient(userService.getByChatId(chatId));
        userService.saveRole(chatId,  userService.getRoleInURL(chatId));
        String role = userService.getRole(chatId);
        switch (role) {
            case "Employee" -> {
                utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.EMPLOYEE), keyboard.nullBtn(chatId, chatId), null);
            }
            case "Contractor", "Admin" -> {
                utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CONTRACTOR), keyboard.panelBtns(role, chatId), null);
            }
            case "Nobody" -> {
                utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.ALREADY_REGISTRATION), keyboard.startBtn(), null);
            }
            case "null" -> {
                utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_INFO_ROLE), null, null);
            }
        }
    }


    public void Employee(Message message) {
        String chatId = message.getChatId().toString();
        String role = userService.getRole(chatId);
        if (role.equals("Nobody") || role.equals("Contractor")) {
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_PERMISSION),null, null);
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
                if (text.startsWith("Код") || text.startsWith("\nШтрих")) {
                    productService.saveCode(message.getText(), chatId);
                }
                utilService.sendMessage(chatId, text, keyboard.nullBtn(chatId, chatId), null);
            } else {
                utilService.sendMessage(chatId, "Something went wrong! (Employee)", null, null);
            }
        } catch (Exception e) {
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.PRODUCT404), null, null);
        }
    }

    public void sendMessageToUser(MessageDTO messageDTO, String username) {
        Users user = userService.getByChatId(messageDTO.getChatId());
        Market market = marketService.getMarketByUserName(username);
        balanceService.saveBalance(messageDTO, market, user);
        utilService.sendMessage(messageDTO.getChatId(), "\uD83C\uDFEA " + market.getName() + "\n" + messageDTO.getText(), keyboard.panelBtns(userService.getRoleInURL(messageDTO.getChatId()), messageDTO.getChatId()), null);
        //        if (Objects.nonNull(messageDTO.getDocumentID())) {
//            sendMessage.setReplyMarkup(keyboard.Query(messageDTO.getChatId()));
//            UpdateUserStatus updateUserStatus = UpdateUserStatus.builder()
//                    .chatId(messageDTO.getChatId())
//                    .Text("\uD83C\uDFEA " + market.getName() + "\n" + messageDTO.getText())
//                    .documentID(messageDTO.getDocumentID())
//                    .clientId(messageDTO.getClientId())
//                    .typeDocument(messageDTO.getTypeDocument())
//                    .status("rejected").build();
//            updateUserStatusService.saveData(updateUserStatus);
        }
//        feign.sendMessage(sendMessage);
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
                utilService.sendMessage(chatId, text, keyboard.panelBtns(userService.getRoleInURL(chatId), chatId), null);
            } else {
                utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_BALANCE), keyboard.panelBtns(userService.getRoleInURL(chatId), chatId), null);
            }
        } catch (Exception e) {
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_BALANCE), keyboard.panelBtns(userService.getRoleInURL(chatId), chatId), null);
        }
    }

    public void getReport(BotState state, String chatId, Integer messageId, String callBackData) {
        try {
            String role = userService.getRole(chatId);
            LocalDate[] dates = new LocalDate[]{LocalDate.now(), LocalDate.now()};
            if (role.equals("Nobody") || role.equals("Employee")) {
                SendMessage sendMessage = new SendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_PERMISSION));
                feign.sendMessage(sendMessage);
                return;
            }
            if (state == BotState.SEND_BY_MONTH) {
                dates = utilService.getFirstAndLastDayOfMonth(callBackData);
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
                reportService.saveEndDate(chatId, callBackData);
            } else {
                reportService.saveStartDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(dates[0]));
                reportService.saveEndDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(dates[1]));
            }
            if (role.equals("Contractor")) {
                sendAllDate(chatId, messageId);
                sendReport(chatId, messageId);
                return;
            }
            if (role.equals("Admin")) {
                sendAllDate(chatId, messageId);
                sendWareHouseButton(state, chatId, messageId);
            }
        } catch (Exception e) {
            System.out.println(LocalDate.now() + " " + userService.getName(chatId) + ", " + chatId + ", File jo`natish bilan bog`liq xatolik, botService.getReport");
        }
    }

    public void sendWareHouseButton(BotState state, String chatId, Integer messageId) {
        ReportDTO reportDto = reportService.getReportDto(chatId);
        try {
            if (state != BotState.SEND_WAREHOUSE_BACK) {
                String text = utilService.getTextByLanguage(chatId, Constant.START_DATE) + reportDto.getStartDate() + "\n" + utilService.getTextByLanguage(chatId, Constant.END_DATE) + reportDto.getEndDate();
                utilService.editMessageText(chatId, messageId, text, null);
            }
        } catch (Exception e) {
            System.out.println(LocalDate.now() + ", " + chatId + ", Davr saqlashda xatolik yuz berdi, botService.sendAllDate");
        }
        if (reportDto.getTypeReport().equals("CASH_BOX")) {
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CHOOSE_CASH_BOX), keyboard.cashBox(chatId), null);
        } else if (reportDto.getTypeReport().equals("DC") || reportDto.getTypeReport().startsWith("AKT") || reportDto.getTypeReport().startsWith("COST")) {
            ReplyKeyboardMarkup branch = keyboard.branch(chatId);
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.ENTER_CONTRACTOR), branch, null);
        } else {
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CHOOSE_WAREHOUSE), keyboard.wareHouse(chatId), null);
        }
    }

    public void sendStartDate(BotState state, String chatId) {
        String role = userService.getRole(chatId);
        if (role.equals("Nobody") || role.equals("Employee")) {
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_PERMISSION), null, null);
            return;
        }
        if (state == BotState.GET_START_DATEV2) {
            reportService.saveNew(chatId, utilService.getTextByLanguage(chatId, Constant.AKT_SVERKA));
        } else if (state == BotState.GET_START_DATE)
            reportService.saveNew(chatId, utilService.getTextByLanguage(chatId, Constant.AKT_SVERKA_TOVAR));
        else if (state == BotState.GET_START_DATE_WAREHOUSE)
            reportService.saveNew(chatId, "WAREHOUSE");
        else if (state == BotState.GET_START_DATE_TRADE)
            reportService.saveNew(chatId, "TRADE");
        else if (state == BotState.GET_START_DATE_TRADE_ALKAN)
            reportService.saveNew(chatId, "TRADE_ALKAN");
        else if (state == BotState.GET_START_DATE_CASH_BOX)
            reportService.saveNew(chatId, "CASH_BOX");
        else if (state == BotState.GET_START_DATE_COST)
            reportService.saveNew(chatId, "COST");
        else if (state == BotState.GET_PERIOD) {
            reportService.saveNew(chatId, "DC");
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CHOOSE_PERIOD), null, keyboard.calendarBtns(LocalDate.now().toString()));
            return;
        }
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CHOOSE_PERIOD), null, keyboard.periodKeyboards(chatId));
    }

    public void saveStartDate(Message message, String chatId, String callbackQuery) {
        ReportDTO reportDto = reportService.getReportDto(chatId);

        if (reportDto.getTypeReport().equals("DC")) {
            reportService.savePeriod(chatId, callbackQuery);

            String text = utilService.getTextByLanguage(chatId, Constant.PERIOD) + callbackQuery;
            utilService.editMessageText(chatId, message.getMessageId(), text, null);

            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.MAIN_MENU), keyboard.typeContractor(chatId), null);
            return;
        }
        reportService.saveStartDate(chatId, callbackQuery);

        String text = utilService.getTextByLanguage(chatId, Constant.START_DATE) + callbackQuery + "\n|\n" +
                utilService.getTextByLanguage(chatId, Constant.CHOOSE_END_DATE);
        utilService.editMessageText(chatId, message.getMessageId(), text, keyboard.calendarBtns(LocalDate.now().toString()));
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
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.ERROR_CHOOSE_DATE), keyboard.panelBtns(userService.getRole(chatId), chatId), null);
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
            ReportDTO dateDto = reportService.getReportDto(chatId);
            if (dateDto.getTypeFile().equals("jpg")) {
                sendReportAsImage(chatId, messageId);
            } else {
                ReportDTO dateDTO = reportService.getReportDto(chatId);
                File reports = api1CService.getReports(dateDTO);
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
        } catch (Exception e) {
            sendClient404Error(chatId, messageId);
        }
    }

    public void sendReportAsImage(String chatId, Integer messageId) {
        try {
            reportService.saveTypeFile(chatId, "pdf");
            ReportDTO dateDTO = reportService.getReportDto(chatId);
            File reports = api1CService.getReports(dateDTO);
            PDF2IMAGE(reports.getAbsolutePath(), messageId, chatId);
            reportService.saveTypeFile(chatId, "jpg");
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
            System.out.println(e + "Xatolik botService().SendCheque");
        }
    }


    public void PDF2IMAGE(String filePath, Integer messageId, String chatId) throws IOException {
        PdfDocument pdf = new PdfDocument();
        pdf.loadFromFile(filePath);
        for (int i = 0; i < pdf.getPages().getCount(); i++) {
            BufferedImage image = pdf.saveAsImage(i, PdfImageType.Bitmap, 500, 500);
            Path path = Paths.get("IMAGE");
            path = utilService.checkPackage(path);
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
            BufferedImage image = pdf.saveAsImage(i, PdfImageType.Bitmap, 500, 500);
            Path path = Paths.get("CHEQUE");
            path = utilService.checkPackage(path);
            File file = new File(path.toFile().getAbsolutePath() + "/" + String.format(("cheque-%d.png"), i));
            ImageIO.write(image, "PNG", file);
            sendChequeAsPhoto(salesReceipt, file);
        }
        pdf.close();
    }

    private void sendClient404Error(String chatId, Integer messageId) {
        utilService.editMessageText(chatId, messageId, utilService.getTextByLanguage(chatId, Constant.ERROR_500), null);
    }


    public void sendAllDate(String chatId, Integer messageId) {
        try {
            ReportDTO dateDTO = reportService.getReportDto(chatId);

            String text = utilService.getTextByLanguage(chatId, Constant.START_DATE) + dateDTO.getStartDate() + "\n" + utilService.getTextByLanguage(chatId, Constant.END_DATE) + dateDTO.getEndDate();
            utilService.editMessageText(chatId, messageId, text, null);
        } catch (Exception e) {
            System.out.println(LocalDate.now() + ", " + chatId + ", Davr saqlashda xatolik yuz berdi, botService.sendAllDate");
        }
    }

    public void sendError(String chatId, Integer messageId) {
        utilService.editMessageText(chatId, messageId, utilService.getTextByLanguage(chatId, Constant.ERROR_CHOOSE_DATE), null);
    }


    public void saveName(Message message, String chatId) {

        String name = userService.getName(chatId);
        if (!utilService.containsSpecialCharacters(message.getText())) {
            if (utilService.containsSpecialCharacters(name)) {
                userService.saveUserName(message, chatId);
            }else if (!getPhone(chatId)){
                utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.REGISTRATION), keyboard.createContactMarkup(chatId), null);
                return;
            }
        }else {
            if (utilService.containsSpecialCharacters(name)){
                utilService.sendMessage(chatId, "Илтимос, исмингизни юборинг...", null, null);
                return;
            }else if (!getPhone(chatId)){
                utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.REGISTRATION), keyboard.createContactMarkup(chatId), null);
                return;
            }
        }
        if (!getPhone(chatId)) {
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.REGISTRATION), keyboard.createContactMarkup(chatId), null);
        }
    }

    public void sendSettings(String chatId) {
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.MAIN_MENU), keyboard.settings(chatId), null);
    }

    public void editTypeFile(BotState state, String chatId, Integer messageId, String callBackData) {
        String role = userService.getRole(chatId);
        if (role.equals("Nobody")) {
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_PERMISSION), null, null);
            return;
        }
        if (state == BotState.EDIT2XLSX) {
            reportService.saveTypeFile(chatId, "xlsx");
            fileService.saveTypeFile(chatId, "xlsx");
        } else if (state == BotState.EDIT2PDF) {
            reportService.saveTypeFile(chatId, "pdf");
            fileService.saveTypeFile(chatId, "pdf");
        } else if (state == BotState.EDIT2JPG) {
            reportService.saveTypeFile(chatId, "jpg");
            fileService.saveTypeFile(chatId, "jpg");
        }
        try {
            utilService.editMessageText(chatId, messageId, utilService.getTextByLanguage(chatId, Constant.CHANGE_TYPE_FILE_200) + callBackData, null);
        } catch (Exception e) {
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CHANGE_TYPE_FILE_200)+callBackData, keyboard.settings(chatId), null);
        }
    }


    public void sendStartDateAsCalendar(Message message, String chatId) {
        String role = userService.getRole(chatId);
        if (role.equals("Nobody") || role.equals("Employee")) {
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_PERMISSION), null, null);
            return;
        }
        try {
            utilService.editMessageText(chatId, message.getMessageId(), utilService.getTextByLanguage(chatId, Constant.CHOOSE_START_DATE), keyboard.calendarBtns(LocalDate.now().toString()));
        } catch (Exception e) {
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CHOOSE_START_DATE), null, keyboard.calendarBtns(LocalDate.now().toString()));
        }
    }

    public void sendTypeFile(String chatId) {
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CHOOSE_FORMAT), null, keyboard.typeFileButtons(chatId));
    }

    public void sendLanguage(String chatId) {
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CHOOSE_FORMAT), null, keyboard.chooseLanguage());
    }

    public void editLanguage(BotState state, String chatId, Integer messageId, String callBackData) {
        String role = userService.getRole(chatId);
        SendMessage sendMessage = new SendMessage();
        if (role.equals("Nobody")) {
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_PERMISSION), null, null);
            return;
        }
        if (state == BotState.EDIT2UZ) {
            userService.saveLanguage(chatId, "uz");
        } else if (state == BotState.EDIT2RU) {
            userService.saveLanguage(chatId, "ru");
        } else if (state == BotState.EDIT2KRIL) {
            userService.saveLanguage(chatId, "kril");
        }
        utilService.editMessageText(chatId, messageId, utilService.getTextByLanguage(chatId, Constant.CHANGE_LANGUAGE_200) + callBackData, new InlineKeyboardMarkup());
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.TO_BE_CONTINUED), keyboard.settings(chatId), null);
    }

    public void sendMainMenu(String chatId) {
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.MAIN_MENU), keyboard.panelBtns(userService.getRoleInURL(chatId), chatId), null);
    }

    public void sendContinue(String chatId) {
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.MAIN_MENU), null, null);
    }

    public void sendCheckingMoney(String chatId, Integer messageID, BotState state, Message message) {
        UpdateUserStatus data = updateUserStatusService.getData(chatId, message.getText());
        if (state == BotState.SEND_AGREED_MONEY) {
            Integer response = postStatus(data, "agreed");
            if (response == 200) {
                try {
                    utilService.editMessageText(chatId, messageID, utilService.getTextByLanguage(chatId, Constant.ACCEPT), null);
                } catch (Exception e) {
                    System.out.println("Cannot edit message text yes/no");
                }
            }
        } else if (state == BotState.SEND_REJECTED_MONEY) {
            Integer response = postStatus(data, "rejected");
            if (response == 200) {
                try {
                    utilService.editMessageText(chatId, messageID, utilService.getTextByLanguage(chatId, Constant.REJECT), null);
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
                    .exchange(Constant.REQUEST_URI + "/bot/update/status", HttpMethod.POST, entity, byte[].class);

            return response.getStatusCodeValue();
        } catch (Exception e) {
            System.out.println(LocalDate.now() + " " + data.getText() + ", " + ", problem");
        }
        return 200;
    }

    public void saveWarehouse(String chatId, Message message) {
        ReportDTO reportDto = reportService.getReportDto(chatId);
        SendMessage sendMessage = new SendMessage();
        if (reportDto.getTypeReport().equals("CASH_BOX")) {
            getReportCashBox(chatId, message);
        } else if (reportDto.getTypeReport().equals("DC")) {
            getReportBranch(chatId, message);
        } else if (reportDto.getTypeReport().startsWith("AKT")) {
            getClients(chatId, message);
        } else if (reportDto.getTypeReport().startsWith("COST")) {
            getReportCost(chatId, message);
        } else {
            sendMessage.setChatId(chatId);
            sendMessage.setReplyMarkup(keyboard.chooseProduct(chatId));
            String text = utilService.getTextByLanguage(chatId, Constant.WAREHOUSE) + ": ";
            sendMessage.setText(text + "\n" + utilService.getTextByLanguage(chatId, Constant.MAIN_MENU));
            if (message.getText().equals("Tovar tanlash")) {
                sendMessage.setText(text + "\n\n" + utilService.getTextByLanguage(chatId, Constant.MAIN_MENU));
                Optional<SearchDTO> byChatId = searchRepository.findByChatId(chatId);
                if (byChatId.isPresent()) {
                    byChatId.get().setPage(1);
                    searchRepository.save(byChatId.get());
                }
//                reportService.saveWareHouseId(chatId, new WarehouseDTO("", "", Boolean.FALSE));
            } else if (message.getText().equals(utilService.getTextByLanguage(chatId, Constant.BACK_STATE))) {
                text = utilService.getTextByLanguage(chatId, Constant.WAREHOUSE) + ": " + warehouseService.getByWarehouseID(reportService.getReportDto(chatId).getWarehouseID());
                Optional<SearchDTO> byChatId = searchRepository.findByChatId(chatId);
                if (byChatId.isPresent()) {
                    byChatId.get().setPage(1);
                    searchRepository.save(byChatId.get());
                }
                sendMessage.setText(text + "\n" + utilService.getTextByLanguage(chatId, Constant.MAIN_MENU));
            } else if (warehouseService.getByClick(message.getText())) {
                text = utilService.getTextByLanguage(chatId, Constant.WAREHOUSE) + ": " + message.getText();
                sendMessage.setText(text + "\n" + utilService.getTextByLanguage(chatId, Constant.MAIN_MENU));
//                reportService.saveWareHouseId(chatId, new WarehouseDTO(message.getText(), warehouseService.getByName(message.getText()), Boolean.FALSE));
            }
            try {
                feign.sendMessage(sendMessage);
            }catch (Exception e){
                System.out.println("\n\n\nError saveWarehouse() method in feign condition\n\n\n......");
            }
        }
    }

    private void getReportCost(String chatId, Message message) {
        reportService.saveBranchID(chatId, warehouseService.getByNameIsBranch(message.getText()));
        utilService.sendMessage(chatId, uiTextCost(reportService.getReportDto(chatId), chatId), keyboard.panelBtns(userService.getRole(chatId), chatId), null);
        sendReportWarehouse(reportService.getReportDto(chatId), chatId);
    }

    public String uiTextCost(ReportDTO reportDTO, String chatId) {
        return utilService.getTextByLanguage(chatId, Constant.START_DATE) + reportDTO.getStartDate() + "\n"
                + utilService.getTextByLanguage(chatId, Constant.END_DATE) + reportDTO.getEndDate() + "\n\n"
                + utilService.getTextByLanguage(chatId, Constant.BRANCH_NAME) + utilService.getTextByLanguage(chatId, warehouseService.getByWarehouseID(reportDTO.getWarehouseID()));
    }

    private void getClients(String chatId, Message message) {
        reportService.saveBranchID(chatId, warehouseService.getByNameIsBranch(message.getText()));
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, utilService.getTextByLanguage(chatId, Constant.BRANCH_NAME) + message.getText() + "\n\n" + Constant.ENTER_CONTRACTOR), keyboard.getContractors(chatId), null);
    }


    public void getByProductGroup(BotState state, String chatId) {
        String text = "";
        if (state == BotState.GET_BY_PRODUCT_GROUP) {
            text = utilService.getTextByLanguage(chatId, Constant.ENTER_PRODUCT_GROUP);
        } else if (state == BotState.GET_BY_PRODUCT) {
            text = utilService.getTextByLanguage(chatId, Constant.ENTER_PRODUCT);
        }
        utilService.sendMessage(chatId, text, keyboard.getAllGroupOfProducts(state, chatId), null);
    }

    public void getByProductGroupByAPI(BotState state, String chatId, Message message) {
        SearchDTO searchDTO = reportService.saveSearch( chatId, "", message.getText());
        String text = "";
        if (message.getText().equals(utilService.getTextByLanguage(chatId, Constant.BACK))){
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.MAIN_MENU), keyboard.panelBtns(userService.getRoleInURL(chatId), chatId), null);
            return;
        }
        System.out.println(message.getText());
        if (!message.getText().equals(utilService.getTextByLanguage(chatId, Constant.BACK_STATE))) {
            if (state == BotState.GET_PRODUCT_IN_API) {
                text = utilService.getTextByLanguage(chatId, Constant.ENTER_PRODUCT);
            } else if (state == BotState.GET_CONTRACTOR_IN_API) {
                text = utilService.getTextByLanguage(chatId, Constant.CHOOSE_CONTRACTOR);
            } else if (state == BotState.GET_TANKS) {
                text = utilService.getTextByLanguage(chatId, Constant.CHOOSE_TANK);
            } else {
                text = utilService.getTextByLanguage(chatId, Constant.CHOOSE_PRODUCT_GROUP);
            }
            ReplyKeyboardMarkup replyKeyboardMarkup = keyboard.groupElements(state, chatId, searchDTO);
            utilService.sendMessage(chatId, text, replyKeyboardMarkup, null);
        } else {
            if (state == BotState.GET_CONTRACTOR_IN_API) {
                utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.MAIN_MENU), keyboard.branch(chatId), null);
                return;
            }
            if (state == BotState.GET_TANKS){
                utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.MAIN_MENU), keyboard.panelBtns(userService.getRoleInURL(chatId), chatId), null);
                return;
            }
            saveWarehouse(chatId, message);
        }
    }

    public void getReportWarehouse(BotState state, String chatId, Message message) {
        if (state!=BotState.GET_REPORT_ANALYSIS){
            ReportDTO reportDTO = reportService.saveProductId(state, chatId, message.getText());
            utilService.sendMessage(chatId, uiText(reportDTO, chatId, message.getText()), keyboard.panelBtns(userService.getRole(chatId), chatId), null);
            sendReportWarehouse(reportDTO, chatId);
        }else {
            SearchDTO searchDTO = reportService.getTankTankID(chatId, message.getText());
            String response = api1CService.sendReportTank(chatId, searchDTO);
            if (response.equals("ERROR"))
                utilService.sendMessage(chatId, "Xatolik yuz berdi", keyboard.panelBtns(userService.getRoleInURL(chatId), chatId), null);
            else
                utilService.sendMessage(chatId, response, keyboard.panelBtns(userService.getRoleInURL(chatId), chatId), null);
        }
    }



    public String uiText(ReportDTO reportDTO, String chatId, String messageText) {
        return utilService.getTextByLanguage(chatId, Constant.START_DATE) + reportDTO.getStartDate() + "\n"
                + utilService.getTextByLanguage(chatId, Constant.END_DATE) + reportDTO.getEndDate() + "\n\n"
                + utilService.getTextByLanguage(chatId, Constant.WAREHOUSE) + ": " + utilService.getTextByLanguage(chatId, warehouseService.getByWarehouseID(reportDTO.getWarehouseID())) + "\n\n" +
                utilService.getTextByLanguage(chatId, Constant.PRODUCT_NAME) + messageText;
    }

    public void getReportSVERKA(String chatId, Message message) {
        ReportDTO reportDTO = reportService.saveClientId(chatId, message.getText());
        utilService.sendMessage(chatId, uiTextSverka(reportDTO, chatId, message.getText()), keyboard.panelBtns(userService.getRole(chatId), chatId), null);
        sendReportWarehouse(reportDTO, chatId);
    }

    public String uiTextSverka(ReportDTO reportDTO, String chatId, String messageText) {
        return utilService.getTextByLanguage(chatId, Constant.START_DATE) + reportDTO.getStartDate() + "\n"
                + utilService.getTextByLanguage(chatId, Constant.END_DATE) + reportDTO.getEndDate() + "\n\n"
                + utilService.getTextByLanguage(chatId, Constant.BRANCH_NAME)  + utilService.getTextByLanguage(chatId, warehouseService.getByWarehouseID(reportDTO.getWarehouseID())) + "\n\n" +
                utilService.getTextByLanguage(chatId, Constant.CLIENT) + ": " + messageText;
    }

    public void getReportCashBox(String chatId, Message message) {
        ReportDTO reportDTO = reportService.saveCashID(chatId, message.getText());
        utilService.sendMessage(chatId, uiTextCashBox(reportDTO, chatId, message.getText()), keyboard.panelBtns(userService.getRole(chatId), chatId), null);
        sendReportWarehouse(reportDTO, chatId);
    }

    public String uiTextCashBox(ReportDTO reportDTO, String chatId, String messageText) {
        return utilService.getTextByLanguage(chatId, Constant.START_DATE) + reportDTO.getStartDate() + "\n"
                + utilService.getTextByLanguage(chatId, Constant.END_DATE) + reportDTO.getEndDate() + "\n\n"
                + utilService.getTextByLanguage(chatId, Constant.CASHBOX_NAME) + messageText;
    }

    public void getReportBranch(String chatId, Message message) {
        reportService.saveBranchID(chatId, warehouseService.getByNameIsBranch(message.getText()));
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.BRANCH_NAME) + message.getText(), keyboard.typeContractor(chatId), null);
    }

        public void sendReportWarehouse(ReportDTO reportDTO, String chatId) {
        Optional<SearchDTO> dto = searchRepository.findByChatId(chatId);
        dto.ifPresent(searchDTO -> searchDTO.setPage(1));
        try {
            String uri = "https://api.telegram.org/bot" + Security.BOT_TOKEN + "/sendDocument";
            HttpPost httppost = new HttpPost(uri);
            File reports = api1CService.getReports(reportDTO);
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
        } catch (Exception e) {
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_INFO), null, null);
        }
    }

    public void getAccountDebt(String chatId, Message message) {
        if (message.getText().equals(utilService.getTextByLanguage(chatId, Constant.POSTAVSHIK)))
            reportService.saveTypeContractor(chatId, 1);
        else if (message.getText().equals(utilService.getTextByLanguage(chatId, Constant.CLIENT))) {
            reportService.saveTypeContractor(chatId, 2);
        } else if (message.getText().equals(utilService.getTextByLanguage(chatId, Constant.ALL))) {
            reportService.saveTypeContractor(chatId, 3);
        }
        ReportDTO reportDto = reportService.getReportDto(chatId);
        String text = utilService.getTextByLanguage(chatId, Constant.PERIOD) + reportDto.getPeriod() + "\n\n"
                + utilService.getTextByLanguage(chatId, Constant.BRANCH_NAME) + warehouseService.getByWarehouseID(reportDto.getBranchID()) + "\n\n"
                + utilService.getTextByLanguage(chatId, Constant.TYPE_CONTRACTOR) + message.getText();

        utilService.sendMessage(chatId, text, keyboard.panelBtns(userService.getRole(chatId), chatId), null);
        sendReportWarehouse(reportDto, chatId);
    }

    public void next(String chatId, Message message) {
        Optional<SearchDTO> searchDTO = searchRepository.findByChatId(chatId);
        if (searchDTO.isPresent()) {
            if (message.getText().equals("-->>")) {
                searchDTO.ifPresent(dto -> dto.setPage(dto.getPage() + 1));
            } else if (searchDTO.get().getPage() != 1) {
                searchDTO.ifPresent(dto -> dto.setPage(dto.getPage() - 1));
            }
            SearchDTO save = searchRepository.save(searchDTO.get());
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.PAGE) + save.getPage(), keyboard.groupElements(userService.getState(chatId), chatId, save), null);
        }
    }

    public void testInlineMode(Update update) {
        String query = update.getInlineQuery().getQuery();
        List<InlineQueryResult> inlineQueryResults = processInlineQuery(query);
        sendInlineQueryResults(update.getInlineQuery().getId(), inlineQueryResults);
    }

    private List<InlineQueryResult> processInlineQuery(String query) {
        List<InlineQueryResult> resultList = new ArrayList<>();

        InlineQueryResultArticle result1 = InlineQueryResultArticle.builder()
                .title(query+"  "+LocalDate.now())
                .id("1")
                .description("Strengthens your back, glutes, and legs.")
                .inputMessageContent(new InputTextMessageContent("Hello")).build();
        resultList.add(result1);

        InlineQueryResultArticle result2 = InlineQueryResultArticle.builder()
                .title("Bench Press")
                .id("2")
                .description("Targets your chest, shoulders, and triceps.")
                .inputMessageContent(new InputTextMessageContent("Hello")).build();
        resultList.add(result2);


        return resultList;
    }

    private void sendInlineQueryResults(String inlineQueryId, List<InlineQueryResult> results) {


        AnswerInlineQuery answerInlineQuery = AnswerInlineQuery.builder()
                .inlineQueryId(inlineQueryId)
                .results(results).build();

        try {
            feign.answerInlineQuery(answerInlineQuery);
        }catch (Exception e){
            System.out.println(e);
        }
    }
}