package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.NoopUserTokenHandler;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import uz.dataFin.notificationbot.feign.TelegramFeign;
import uz.dataFin.notificationbot.helper.Keyboard;
import uz.dataFin.notificationbot.model.*;
import uz.dataFin.notificationbot.repository.AppealRepository;
import uz.dataFin.notificationbot.utils.BotState;
import uz.dataFin.notificationbot.utils.Constant;
import uz.dataFin.notificationbot.utils.Security;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FeignService {
    private final UtilService utilService;
    private final Keyboard keyboard;
    private final UserService userService;
    private final Api1CService api1CService;
    private final ReportService reportService;
    private final ADSService adsService;
    private final TelegramFeign feign;
    private final AppealRepository appealRepository;


    public void sendName(String chatId) {
        utilService.sendMessage(chatId, "Илтимос, исмингизни юборинг...", null, null);
    }

    public void sendContact(String chatId) {
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.REGISTRATION), keyboard.createContactMarkup(chatId), null);
    }

    public void saveContact(Message message, String chatId) {
        userService.saveContact(message, chatId);
        api1CService.saveClient(userService.getByChatId(chatId));
        switch (userService.getRole(chatId)) {
            case "Employee" -> {
                utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.EMPLOYEE), null, null);
            }
            case "Contractor" -> {
                utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CONTRACTOR), keyboard.panelBtns(userService.getRole(chatId), chatId), null);
            }
            case "Nobody" -> {
                utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.ALREADY_REGISTRATION), keyboard.panelBtns(userService.getRole(chatId), chatId), null);
            }
            case "null" -> {
                utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.NO_INFO_ROLE), null, null);
            }
        }
    }

    public void sendDate(String chatId) {
        if (!reportService.getReportDto(chatId).getTypeReport().equals("DC"))
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CHOOSE_PERIOD), null, keyboard.periodKeyboards(chatId));
        else
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.PERIOD), null, keyboard.calendarBtns(LocalDate.now().toString()));

    }

    public void editLanguage(BotState state, String chatId, Integer messageId, String callBackData) {
        switch (state) {
            case EDIT2UZ -> userService.saveLanguage(chatId, "uz");
            case EDIT2KRIL -> userService.saveLanguage(chatId, "kril");
            case EDIT2RU -> userService.saveLanguage(chatId, "ru");
        }
        utilService.editMessageText(chatId, messageId, utilService.getTextByLanguage(chatId, Constant.CHANGE_LANGUAGE_200) + callBackData, new InlineKeyboardMarkup());
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.TO_BE_CONTINUED), keyboard.settings(chatId), null);
    }

    public void saveDates(BotState state, String chatId, Integer messageId, String callBackData) {
        try {
            LocalDate[] dates = new LocalDate[]{LocalDate.now(), LocalDate.now()};
            switch (state) {
                case SEND_BY_MONTH -> dates = utilService.getFirstAndLastDayOfMonth(callBackData);
                case SEND_BY_WEEK -> dates = utilService.getBeginningOfWeekAndToday();
                case SEND_BY_LAST_MONTH -> dates = UtilService.getFirstDayOfMonthAndToday();
                case SEND_BY_SEASON -> dates = UtilService.getFirstDayOfQuarterAndToday();
                case SEND_BY_YEAR -> dates = UtilService.getFirstDayOfYearAndToday();
            }

            if (state == BotState.REPORTS) {
                reportService.saveEndDate(chatId, callBackData);
            } else {
                reportService.saveStartDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(dates[0]));
                reportService.saveEndDate(chatId, DateTimeFormatter.ofPattern("yyyy-MM-dd").format(dates[1]));
            }

            switch (userService.getRole(chatId)){
                case "Contractor" -> {sendAllDate(chatId, messageId); sendReport(chatId);}
                case "Nobody" -> {utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.ALREADY_REGISTRATION), keyboard.startBtn(), null);}

            }
        } catch (Exception e) {
            System.out.println(LocalDate.now() + " " + userService.getName(chatId) + ", " + chatId + ", File jo`natish bilan bog`liq xatolik, botService.getReport");
        }
    }

    private void sendAllDate(String chatId, Integer messageId) {
        try {
            Report dateDTO = reportService.getReportDto(chatId);
            String text = utilService.getTextByLanguage(chatId, Constant.START_DATE) + dateDTO.getStartDate() + "\n" + utilService.getTextByLanguage(chatId, Constant.END_DATE) + dateDTO.getEndDate();
            utilService.editMessageText(chatId, messageId, text, null);
        } catch (Exception e) {
            System.out.println(LocalDate.now() + ", " + chatId + ", Davr saqlashda xatolik yuz berdi, botService.sendAllDate");
        }
    }


    public void sendReport(String chatId) {
        Report report = reportService.getReportDto(chatId);
        userService.updateUserState(chatId, BotState.DEFAULT);
        try {
            String uri = "https://api.telegram.org/bot" + Security.BOT_TOKEN + "/sendDocument";
            HttpPost httppost = new HttpPost(uri);
            File reports = api1CService.getReports(report);
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
            utilService.sendMessage(chatId, e+utilService.getTextByLanguage(chatId, Constant.NO_INFO), null, null);
        }
    }


    public void sendPanelBTNS(String chatId) {
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.MAIN_MENU), keyboard.panelBtns(userService.getRole(chatId), chatId), null);
    }

    public void sendStartDateAsCalendar(Message message, String chatId) {
        try {
            utilService.editMessageText(chatId, message.getMessageId(), utilService.getTextByLanguage(chatId, Constant.CHOOSE_START_DATE), keyboard.calendarBtns(LocalDate.now().toString()));
        } catch (Exception e) {
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CHOOSE_START_DATE), null, keyboard.calendarBtns(LocalDate.now().toString()));
        }
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

    public void saveStartDate(Message message, String chatId, String callBackData) {
        reportService.saveStartDate(chatId, callBackData);
        utilService.editMessageText(chatId, message.getMessageId(), utilService.getTextByLanguage(chatId, Constant.CHOOSE_END_DATE), keyboard.calendarBtns(LocalDate.now().toString()));
    }


    public void sendSettings(String chatId) {
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.MAIN_MENU), keyboard.settings(chatId), null);
    }

    public void sendTypeFile(String chatId) {
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CHOOSE_FORMAT), null, keyboard.typeFileButtons(chatId));
    }

    public void sendLanguage(String chatId) {
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CHOOSE_LANGUAGE), null, keyboard.chooseLanguage());
    }

    public void saveTypeFile(BotState state, String chatId, Integer messageId, String callBackData) {
        switch (state){
            case EDIT2XLSX -> reportService.saveTypeFile(chatId, "xlsx");
            case EDIT2PDF -> reportService.saveTypeFile(chatId, "pdf");
            case EDIT2JPG -> reportService.saveTypeFile(chatId, "jpg");
        }

        try {
            utilService.editMessageText(chatId, messageId, utilService.getTextByLanguage(chatId, Constant.CHANGE_TYPE_FILE_200) + callBackData, null);
        } catch (Exception e) {
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CHANGE_TYPE_FILE_200)+callBackData, keyboard.settings(chatId), null);
        }
    }

    public void sendBalance(String chatId) {
        utilService.sendMessage(chatId, api1CService.getBalance(chatId), keyboard.panelBtns(userService.getRoleInURL(chatId), chatId), null);
    }

    public void sendChoosingDateError(String chatId, Integer messageId) {
        try {
            utilService.editMessageText(chatId, messageId, utilService.getTextByLanguage(chatId, Constant.ERROR_CHOOSE_DATE), null);
        } catch (Exception e) {
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.ERROR_CHOOSE_DATE), keyboard.panelBtns(userService.getRoleInURL(chatId), chatId), null);
        }
    }

    public void sendAds(String chatId) {
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.SEND_ADS_TEXT), null, null);
    }

    public void savePhotoOrCaption(String chatId, Message message) {
        if (userService.getRoleInURL(chatId).equals("Employee")) {
            List<PhotoSize> photos = message.getPhoto();
            String caption = (Objects.nonNull(message.getCaption()))?message.getCaption():"";
            PhotoSize photo = photos.stream()
                    .max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElse(null);
            assert photo != null;
            adsService.saveAdsById(chatId, caption, photo.getFileId(), true);

            sendImage(chatId, photo.getFileId(), caption);

            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.EDIT_CAPTION), null, keyboard.editCaption(chatId));

        }
    }

    private void sendImage(String chatId, String fileId, String caption) {
        try {
            String uri = "https://api.telegram.org/bot" + Security.BOT_TOKEN + "/sendPhoto";
            HttpPost httppost = new HttpPost(uri);

            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("chat_id", chatId));
            params.add(new BasicNameValuePair("photo", fileId));
            params.add(new BasicNameValuePair("caption", caption));
            httppost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

            try (CloseableHttpClient httpClient = HttpClients.createDefault();
                 CloseableHttpResponse response = httpClient.execute(httppost)) {
                System.out.println(response);
            } catch (IOException e) {
                System.out.println(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void getAnotherAds(String chatId, Integer messageId, String callBackData) {
        if (userService.getRoleInURL(chatId).equals("Employee")) {
            try {
                if (!callBackData.equals("NOYES")) {
                    utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.ADS_TEXT), keyboard.panelBtns(userService.getRoleInURL(chatId), chatId), null);
                } else {
                    utilService.editMessageText(chatId, messageId, utilService.getTextByLanguage(chatId, Constant.ADS_TEXT), null);
                }
            }catch (Exception e){
                utilService.sendMessage(chatId, "NOTO'G'RI BUYRUQ", keyboard.panelBtns(userService.getRoleInURL(chatId), chatId), null);
            }
        }else {
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.MAIN_MENU), keyboard.panelBtns(userService.getRoleInURL(chatId), chatId), null);
        }
    }

    public void sendUsersAds(String chatId, Integer messageId, String callBackData) {
        if (userService.getRoleInURL(chatId).equals("Employee")) {
            List<Users> all = userService.getAll();
            Optional<Advertising> advertising = adsService.findByClientID(chatId);
            if (advertising.isPresent()) {
                for (Users users : all) {
                    if (users.getChatId().equals(chatId))
                        continue;
                    try {
                        if (Objects.nonNull(advertising.get().getFilePath())) {
                            if (advertising.get().getIsPhoto()) {
                                sendImage(users.getChatId(), advertising.get().getFilePath(), advertising.get().getCaption());
                            }else {
                                sendVideo(users.getChatId(), advertising.get().getFilePath(), advertising.get().getCaption());
                            }
                        }else {
                            SendMessage sendMessage = new SendMessage(users.getChatId(), advertising.get().getCaption());
                            assert feign != null;
                            feign.sendMessage(sendMessage);
                        }
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
            }

            if (callBackData.equals("YES")) {
                utilService.editMessageText(chatId, messageId, utilService.getTextByLanguage(chatId, Constant.SEND_ADS_200), null);
            }
        }else{
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.MAIN_MENU), keyboard.panelBtns(userService.getRoleInURL(chatId), chatId), null);
        }
    }

    private void sendVideo(String chatId, String fileId, String caption) {
        try {
            String uri = "https://api.telegram.org/bot" + Security.BOT_TOKEN + "/sendVideo";
            HttpPost httppost = new HttpPost(uri);


            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("chat_id", chatId));
            params.add(new BasicNameValuePair("video", fileId));
            params.add(new BasicNameValuePair("caption", caption));
            httppost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

            try (CloseableHttpClient httpClient = HttpClients.createDefault();
                 CloseableHttpResponse response = httpClient.execute(httppost)) {
                System.out.println(response);
            } catch (IOException e) {
                System.out.println(e);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void saveVideoOrCaption(String chatId, Message message) {
        if (userService.getRoleInURL(chatId).equals("Employee")) {
            Video video = message.getVideo();
            String caption = (Objects.nonNull(message.getCaption()))?message.getCaption():"";
            adsService.saveAdsById(chatId, caption, video.getFileId(), false);

            sendVideo(chatId, video.getFileId(), caption);

            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.EDIT_CAPTION), null, keyboard.editCaption(chatId));

        }
    }

    public void sendAppealText(String chatId) {
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.APPEAL_TEXT), keyboard.panelBtns(userService.getRole(chatId), chatId), null);
        userService.updateUserState(chatId, BotState.GET_APPEAL_TEXT);
    }

    public void checkAppealText(String chatId, String text) {
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CHECK_APPEAL_TEXT ) + templateForAppealText(userService.getByChatId(chatId), text), null, keyboard.editCaption(chatId));
        userService.updateUserState(chatId, BotState.CHECK_APPEAL);
        appealRepository.save(new Appeals(text, chatId, userService.getName(chatId), "NEW"));
    }

    public void sendAppealGroup(List<Groups> groups, Update update) {
        try {
            String chatId = utilService.getChatIdFromUpdate(update);
            String appealText = "null";
            if (appealRepository.findTopByClientIdOrderByCreatedAtDesc(chatId).isPresent()){
                appealText = appealRepository.findTopByClientIdOrderByCreatedAtDesc(chatId).get().getAppealText();
            }
            for (Groups group : groups) {
                if (group.getStatus().equals("administrator")) {
                    utilService.sendMessage(group.getGroupId(), templateForAppealText(userService.getByChatId(chatId), appealText), null, null);
                    utilService.sendMessage(chatId, "Xabaringiz "+group.getGroupName()+" guruhiga yuborildi!", keyboard.panelBtns(userService.getRole(chatId), chatId), null);
                }
            }
            utilService.editMessageText(chatId, utilService.getMessageIdFromUpdate(update), templateForAppealText(userService.getByChatId(chatId), appealText) + "\n\n"+utilService.getTextByLanguage(chatId, Constant.STATUS_YES), null);
            userService.updateUserState(chatId, BotState.DEFAULT);
        }catch (Exception e) {
            System.out.println(e);
        }
    }

    private String templateForAppealText(Users users, String appealText){
        return utilService.getTextByLanguage(users.getChatId(), Constant.TEMPLATE_APPEAL).replace("<user>", users.getFirstname()).replace("<phone>", users.getPhone()).replace("<date>", LocalDate.now().toString()) + appealText;
    }

    public void rejectAppeal(String chatId, Message message) {
        String appealText = "null";
        if (appealRepository.findTopByClientIdOrderByCreatedAtDesc(chatId).isPresent()){
            appealText = appealRepository.findTopByClientIdOrderByCreatedAtDesc(chatId).get().getAppealText();
        }
        utilService.editMessageText(chatId, message.getMessageId(), templateForAppealText(userService.getByChatId(chatId), appealText) + "\n\n"+utilService.getTextByLanguage(chatId, Constant.STATUS_NO), null);
        userService.updateUserState(chatId, BotState.DEFAULT);
    }

    public void sendInfoGroup(Map<Boolean, Groups> groupsMap, Update update) {
        Groups groups = groupsMap.get(true);
        String chatId = utilService.getChatIdFromUpdate(update);
        utilService.sendMessage(chatId, "Guruh nomi: "+groups.getGroupName()+"\nBot huquqi: "+groups.getStatus()+"\nSana: "+LocalDate.now().toString(), keyboard.panelBtns(userService.getRole(chatId), chatId), null);
    }
}
