package uz.dataFin.notificationbot.service;

import com.spire.pdf.PdfDocument;
import com.spire.pdf.graphics.PdfImageType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.feign.TelegramFeign;
import uz.dataFin.notificationbot.helper.Keyboard;
import uz.dataFin.notificationbot.model.DateDTO;
import uz.dataFin.notificationbot.model.Market;
import uz.dataFin.notificationbot.model.MessageDTO;
import uz.dataFin.notificationbot.model.Users;
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


    public BotState getAndCheck(Update update) {
        UserDTO userDTO = userService.checkAndGet(update);
        return userDTO.getState();
    }

    public void saveData(Update update, UserDTO userDTO) {
        Message message = update.hasMessage() ? update.getMessage() : update.getCallbackQuery().getMessage();
        userService.saveData(message.getChatId().toString(), userDTO);
    }

    public void getMainMenuSend(Update update) {
        SendMessage sendMessage = new SendMessage();
        String chatId = utilService.getChatIdFromUpdate(update);
        sendMessage.setChatId(chatId);
        String name = userService.getName(update);
        if (!UtilService.containsSpecialCharacters(name)) {
            sendName(update);
        }else if (!getPhone(update)) {
            sendMessage.setText("Aссалому алайкум " + name + "\n▪Рўйхатдан ўтиш учун телефон рақамингизни юборинг!");
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
                SendMessage sendMessage = new SendMessage(chatId, "Сиз ходимлар рўйхатига қўшилгансиз\n\nКеракли маҳсулот коди ёки шртих кодини киритинг!");
                sendMessage.setReplyMarkup(keyboard.nullBtn(chatId));
                feign.sendMessage(sendMessage);
            }
            case "Contractor" -> {
                SendMessage sendMessage = new SendMessage(chatId, """
                        Сиз контрагентлар рўйхатига қўшилгансиз!
                        
                        /settings -- файл форматини о`згартириш учун ушбу буйруқдан фойдаланинг!
                        
                        Қуйидаги буйруқлардан бирини танланг!""");
                sendMessage.setReplyMarkup(keyboard.panelBtns());
                feign.sendMessage(sendMessage);
            }
            case "Nobody" -> {
                SendMessage sendMessage = new SendMessage(chatId, "Рўйхатдан ўтиш муваффақиятли якунланди✅\n\n" + userService.getName(update) + ", сиз ҳақингизда ҳеч қандай маълумот топилмади!\nМаълумотларингиз тасдиқланишини кутинг.");
                sendMessage.setReplyMarkup(keyboard.startBtn());
                feign.sendMessage(sendMessage);
            }
            case "null" -> {
                SendMessage sendMessage = new SendMessage(chatId, userService.getName(update) + ", сиз ҳақингизда ҳеч қандай маълумот топилмади!\nМаълумотларингиз тасдиқланишини кутинг.");
                feign.sendMessage(sendMessage);
            }
        }
    }


    public void Employee(Update update){
        String chatId = utilService.getChatIdFromUpdate(update);
        String role = userService.getRole(chatId);
        if (role.equals("Nobody") || role.equals("Contractor")){
            SendMessage sendMessage = new SendMessage(chatId, "Буйруқдан фойдаланишга рухсат йўқ!");
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
                sendMessage.setReplyMarkup(keyboard.nullBtn(chatId));
                feign.sendMessage(sendMessage);
            } else {
                SendMessage sendMessage = new SendMessage(chatId, "Something went wrong! (Employee)");
                feign.sendMessage(sendMessage);
            }
        }catch (Exception e){
            SendMessage sendMessage = new SendMessage(chatId, "Ушбу маҳсулот ҳақида маълумот топилмади!");
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
        String chatId = utilService.getChatIdFromUpdate(update);
        String role = userService.getRole(chatId);
        if (role.equals("Contractor")) {
            fileService.saveMethodType(chatId, "BALANCE");
        } else if (role.equals("Employee")) {
            fileService.saveMethodType(chatId, "EMPLOYEE");
        } else {
            SendMessage sendMessage = new SendMessage(chatId, "Буйруқдан фойдаланишга рухсат йўқ!");
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
                SendMessage sendMessage = new SendMessage(chatId, "Сизда баланс бўйича ҳеч қандай маълумот мавжуд эмас!");
                feign.sendMessage(sendMessage);
            }
        }catch (Exception e){
            SendMessage sendMessage = new SendMessage(chatId, "Сизда баланс бўйича ҳеч қандай маълумот мавжуд эмас!");
            feign.sendMessage(sendMessage);
        }
    }

    public void getReport(BotState state, Update update) {
        try {
            String chatId = utilService.getChatIdFromUpdate(update);
            String role = userService.getRole(chatId);
            if (role.equals("Nobody") || role.equals("Employee")){
                SendMessage sendMessage = new SendMessage(chatId, "Буйруқдан фойдаланишга рухсат йўқ!");
                feign.sendMessage(sendMessage);
                return;
            }
            if (state == BotState.REPORTS) {
                fileService.saveEndDate(chatId, update.getCallbackQuery().getData());
                sendAllDate(update);
                sendReport(update);
//                sendReportAsImage(update);
            } else {
                LocalDate[] monthDates = UtilService.getFirstAndLastDayOfMonth(update.getCallbackQuery().getData());
                if (monthDates != null) {
                    LocalDate firstDay = monthDates[0];
                    LocalDate lastDay = monthDates[1];

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    fileService.saveStartDate(chatId, formatter.format(firstDay));
                    fileService.saveEndDate(chatId, formatter.format(lastDay));
                    sendAllDate(update);
                    sendReport(update);
//                    sendReportAsImage(update);
                } else {
                    System.out.println("Oyni tanlashda xatolik yuz berdi");
                }
            }
        } catch (Exception e) {
            System.out.println(LocalDate.now()+" "+userService.getName(update)+", "+utilService.getChatIdFromUpdate(update)+", File jo`natish bilan bog`liq xatolik, botService.getReport");
        }
    }

    public void sendStartDate(BotState state, Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        String role = userService.getRole(chatId);
        if (role.equals("Nobody") || role.equals("Employee")){
            SendMessage sendMessage = new SendMessage(chatId, "Буйруқдан фойдаланишга рухсат йўқ!");
            feign.sendMessage(sendMessage);
            return;
        }
        SendMessage sendMessage = new SendMessage();
        if (state == BotState.GET_START_DATEV2) {
            fileService.saveReportId(chatId, 2);
        }else if (state == BotState.GET_START_DATE)
            fileService.saveReportId(chatId, 1);
        sendMessage.setChatId(chatId);
        sendMessage.setText("⏰Бошланғич санани танланг:");
        sendMessage.setReplyMarkup(keyboard.calendarBtns(LocalDate.now().toString()));
        feign.sendMessage(sendMessage);
    }

    public void saveStartDate( Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        fileService.saveStartDate(chatId, update.getCallbackQuery().getData());
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
        String text = "⏰Бошланғич сана -> "+update.getCallbackQuery().getData()+"\n|\n" +
                "Охирги санани танланг:";
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
            SendMessage sendMessage = new SendMessage(chatId, "\uD83C\uDD98Давр танлашда хатолик юз берди! \n" +
                    "⬅️Жараённи қайтадан бошланг");
            feign.sendMessage(sendMessage);
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
        editMessageText.setText("\uD83D\uDCC3 Server bilan xatolik yuz berdi\\ botService.send404");
        feign.editMessageText(editMessageText);
    }



    public void sendAllDate(Update update){
        try {
            Long chatId = Long.valueOf("1148134936");
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
            String text = "⏰ Бошланғич сана-> " + dateDTO.getStartDate() + "\n" + "⏰ Охирги сана -> " + dateDTO.getEndDate();
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
        editMessageText.setText("\uD83C\uDD98Давр танлашда хатолик юз берди! \n" +
                "⬅️Жараённи қайтадан бошланг");
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
        if (!UtilService.containsSpecialCharacters(name)) {
            userService.saveUserName(update);
        }
        if (!getPhone(update)) {
            sendMessage.setText("Aссалому алайкум " + userService.getName(update) + "\n▪️Рўйхатдан ўтиш учун телефон рақамингизни юборинг!");
            sendMessage.setReplyMarkup(keyboard.createContactMarkup());
            feign.sendMessage(sendMessage);
        }
        if (message.getText().equals("\uD83D\uDCC5АКТ СВEРКА")){
            sendStartDate(BotState.GET_START_DATEV2, update);
            saveData(update, new UserDTO(BotState.GET_START_DATEV2));
            return;
        }
        if (message.getText().equals("\uD83D\uDCB0БАЛАНС")){
            getBalance(update);
            saveData(update, new UserDTO(BotState.GET_BALANCE));
            return;
        }
        if (message.getText().equals("\uD83D\uDCC5АКТ СВEРКА (товар)")){
            sendStartDate(BotState.GET_START_DATE, update);
            saveData(update, new UserDTO(BotState.GET_START_DATE));
            return;
        }
        if (message.getText().equals("/settings")){
            sendTypeFile(update);
            saveData(update, new UserDTO(BotState.SEND_BTN_TYPE_FILE));
            return;
        }
        if (UtilService.containsOnlyNumbers(update.getMessage().getText())){
            Employee(update);
            saveData(update, new UserDTO(BotState.GET_PRODUCT));
        }
    }

    public void sendTypeFile(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Маълумотларни қайси форматда қабул қилмоқчисиз?");
        sendMessage.setReplyMarkup(keyboard.typeFileButtons());
        feign.sendMessage(sendMessage);
    }

    public void editTypeFile(BotState state, Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        String role = userService.getRole(chatId);
        SendMessage sendMessage = new SendMessage(chatId, "Жаражонни давом еттиришингиз мумкин.");
        if (role.equals("Nobody")){
            sendMessage.setText("Буйруқдан фойдаланишга рухсат йўқ!");
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
        editMessageText.setText("Маълумотларингиз муваффақиятли узгартирилди!");
        feign.editMessageText(editMessageText);
        if (role.equals("Contractor")){
            sendMessage.setReplyMarkup(keyboard.panelBtns());
        } else {
            sendMessage.setReplyMarkup(keyboard.nullBtn(chatId));
        }
        feign.sendMessage(sendMessage);
    }
}
