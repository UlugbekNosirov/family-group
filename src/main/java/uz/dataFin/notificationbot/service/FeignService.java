package uz.dataFin.notificationbot.service;

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
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import uz.dataFin.notificationbot.feign.TelegramFeign;
import uz.dataFin.notificationbot.helper.Keyboard;
import uz.dataFin.notificationbot.model.DateDTO;
import uz.dataFin.notificationbot.model.ReportDTO;
import uz.dataFin.notificationbot.model.SalesReceipt;
import uz.dataFin.notificationbot.model.SearchDTO;
import uz.dataFin.notificationbot.repository.FileRepository;
import uz.dataFin.notificationbot.repository.SearchRepository;
import uz.dataFin.notificationbot.utils.BotState;
import uz.dataFin.notificationbot.utils.Constant;
import uz.dataFin.notificationbot.utils.Security;

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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FeignService {
    private final UtilService utilService;
    private final Keyboard keyboard;
    private final UserService userService;
    private final Api1CService api1CService;
    private final ReportService reportService;
    private final WarehouseService warehouseService;
    private final SearchRepository searchRepository;
    private final TelegramFeign feign;


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
                utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.EMPLOYEE), keyboard.nullBtn(chatId, chatId), null);
            }
            case "Contractor", "Admin" -> {
                utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CONTRACTOR), keyboard.panelBtns(userService.getRole(chatId), chatId), null);
            }
            case "Nobody" -> {
                utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.ALREADY_REGISTRATION), keyboard.startBtn(), null);
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

            if (userService.getRole(chatId).equals("Admin")) {
                sendAllDate(chatId, messageId);
                getNextCondition(chatId);
            }
        } catch (Exception e) {
            System.out.println(LocalDate.now() + " " + userService.getName(chatId) + ", " + chatId + ", File jo`natish bilan bog`liq xatolik, botService.getReport");
        }
    }

    private void getNextCondition(String chatId) {
        String typeReport = reportService.getReportDto(chatId).getTypeReport();
        if (typeReport.startsWith("AKT_") || typeReport.equals("DC") || typeReport.equals("COST")) {
            userService.updateUserState(chatId, BotState.GET_BRANCH);
            sendBranches(chatId);
        } else if (typeReport.equals("WAREHOUSE") || typeReport.equals("TRADE") || typeReport.equals("TRADE_ALKAN")) {
            userService.updateUserState(chatId, BotState.GET_WAREHOUSE);
            sendWarehouse(chatId);
        } else if (typeReport.equals("CASH_BOX")) {
            userService.updateUserState(chatId, BotState.GET_CASH_BOX);
            sendCashBox(chatId);
        }
    }

    public void sendWarehouse(String chatId) {
        ReplyKeyboardMarkup branch = keyboard.wareHouse(chatId);
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CHOOSE_WAREHOUSE), branch, null);
    }

    public void sendBranches(String chatId) {
        userService.updateUserState(chatId, BotState.GET_BRANCH);
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.ENTER_BRANCH), keyboard.branch(chatId), null);
    }

    private void sendAllDate(String chatId, Integer messageId) {
        try {
            ReportDTO dateDTO = reportService.getReportDto(chatId);
            String text = utilService.getTextByLanguage(chatId, Constant.START_DATE) + dateDTO.getStartDate() + "\n" + utilService.getTextByLanguage(chatId, Constant.END_DATE) + dateDTO.getEndDate();
            utilService.editMessageText(chatId, messageId, text, null);
        } catch (Exception e) {
            System.out.println(LocalDate.now() + ", " + chatId + ", Davr saqlashda xatolik yuz berdi, botService.sendAllDate");
        }
    }

    public void sendContractors(String chatId, String text) {
        try {
            SearchDTO searchDTO = setTextToSearch(chatId, text);
            ReportDTO dateDTO = reportService.getReportDto(chatId);
            ReplyKeyboardMarkup replyKeyboardMarkup = keyboard.groupElements(BotState.GET_CONTRACTORS, chatId, searchDTO);
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.BRANCH_NAME) + warehouseService.getBranchNameById(dateDTO.getBranchID()) + "\n\n" + utilService.getTextByLanguage(chatId, Constant.ENTER_CONTRACTOR), replyKeyboardMarkup, null);
        } catch (Exception e) {
            System.out.println(LocalDate.now() + ", " + chatId + ", Davr saqlashda xatolik yuz berdi, botService.sendAllDate");
        }
    }

    private SearchDTO setTextToSearch(String chatId, String text) {
        Optional<SearchDTO> searchDTO = searchRepository.findByChatId(chatId);
        if (searchDTO.isPresent()) {
            searchDTO.get().setSearch(text);
            return searchDTO.get();
        }
        return new SearchDTO(chatId, text, 1, 10);
    }

    public void pagination(BotState state, String chatId, Message message) {
        Optional<SearchDTO> searchDTO = searchRepository.findByChatId(chatId);
        if (searchDTO.isPresent()) {
            if (message.getText().equals("-->>")) {
                searchDTO.ifPresent(dto -> dto.setPage(dto.getPage() + 1));
            } else if (searchDTO.get().getPage() != 1) {
                searchDTO.ifPresent(dto -> dto.setPage(dto.getPage() - 1));
            }
            SearchDTO save = searchRepository.save(searchDTO.get());
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.PAGE) + save.getPage(), keyboard.groupElements(state, chatId, save), null);
        }
    }

    public void sendReport(String chatId) {
        ReportDTO reportDTO = reportService.getReportDto(chatId);
        userService.updateUserState(chatId, BotState.DEFAULT);
        sendApprociateText(chatId, reportDTO);
        Optional<SearchDTO> dto = searchRepository.findByChatId(chatId);
        dto.ifPresent(searchDTO -> {
            searchDTO.setPage(1);
            searchRepository.save(dto.get());
        });
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

    private void sendApprociateText(String chatId, ReportDTO reportDTO) {
        String typeReport = reportDTO.getTypeReport();
        if (typeReport.startsWith("AKT_") || typeReport.equals("DC") || typeReport.equals("COST"))
            utilService.sendMessage(chatId, utilService.uiTextSVERKA(chatId, reportDTO, warehouseService.getBranchNameById(reportDTO.getBranchID())), keyboard.panelBtns(userService.getRole(chatId), chatId), null);
        else if (typeReport.equals("WAREHOUSE") || typeReport.equals("TRADE") || typeReport.equals("TRADE_ALKAN")) {
            String warehouseById = warehouseService.getWarehouseById(reportDTO.getWarehouseID());
            String text = utilService.uiTextWAREHOUSE(chatId, reportDTO, warehouseById);
            utilService.sendMessage(chatId, text, keyboard.panelBtns(userService.getRole(chatId), chatId), null);
        } else if (typeReport.equals("CASH_BOX")) {
            String warehouseById = warehouseService.getWarehouseById(reportDTO.getWarehouseID());
            String text = utilService.uiTextCashBox(chatId, reportDTO, warehouseById);
            utilService.sendMessage(chatId, text, keyboard.panelBtns(userService.getRole(chatId), chatId), null);
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
        if (reportService.getReportDto(chatId).getTypeReport().equals("DC")){
            reportService.savePeriod(chatId, callBackData);
            utilService.editMessageText(chatId, message.getMessageId(), utilService.getTextByLanguage(chatId, Constant.PERIOD) + callBackData, null);
            getNextCondition(chatId);
            return;
        }
        String text = utilService.getTextByLanguage(chatId, Constant.START_DATE) + callBackData + "\n|\n" +
                utilService.getTextByLanguage(chatId, Constant.CHOOSE_END_DATE);

        utilService.editMessageText(chatId, message.getMessageId(), text, keyboard.calendarBtns(LocalDate.now().toString()));
    }

    public void sendProductBtns(String chatId) {
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.WAREHOUSE)  + ": "+ warehouseService.getByWarehouseID(reportService.getReportDto(chatId).getWarehouseID()) + "\n" + utilService.getTextByLanguage(chatId, Constant.MAIN_MENU), keyboard.chooseProduct(chatId), null);
    }

    public void sendProductInAPI(String chatId, BotState state) {
        if (state == BotState.GET_PRODUCT_IN_API)
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.ENTER_PRODUCT), keyboard.groupElements(state, chatId, new SearchDTO(chatId, " ", 1, 10)), null);
        else
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.ENTER_PRODUCT_GROUP), keyboard.groupElements(state, chatId, new SearchDTO(chatId, " ", 1, 10)), null);
    }

    public void sendProducts(BotState state, String chatId, String text) {
        try {
            SearchDTO searchDTO = setTextToSearch(chatId, text);
            ReportDTO dateDTO = reportService.getReportDto(chatId);
            ReplyKeyboardMarkup replyKeyboardMarkup = keyboard.groupElements(state, chatId, searchDTO);
            utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.WAREHOUSE) +": "+warehouseService.getByWarehouseID(dateDTO.getWarehouseID()) + "\n\n" + utilService.getTextByLanguage(chatId, Constant.ENTER_PRODUCT), replyKeyboardMarkup, null);
        } catch (Exception e) {
            System.out.println(LocalDate.now() + ", " + chatId + ", Davr saqlashda xatolik yuz berdi, botService.sendAllDate");
        }
    }

    public void sendTypeContractorsBtns(String chatId) {
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.BRANCH_NAME)+warehouseService.getBranchNameById(reportService.getReportDto(chatId).getBranchID()) +"\n"+utilService.getTextByLanguage(chatId, Constant.MAIN_MENU), keyboard.typeContractor(chatId), null);
    }

    public void sendCashBox(String chatId) {
        userService.updateUserState(chatId, BotState.GET_CASH_BOX);
        utilService.sendMessage(chatId, utilService.getTextByLanguage(chatId, Constant.CHOOSE_CASH_BOX), keyboard.cashBox(chatId), null);
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
}
