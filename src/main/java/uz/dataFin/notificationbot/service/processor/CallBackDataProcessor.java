package uz.dataFin.notificationbot.service.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.service.FeignService;
import uz.dataFin.notificationbot.service.ReportService;
import uz.dataFin.notificationbot.service.UserService;
import uz.dataFin.notificationbot.service.UtilService;
import uz.dataFin.notificationbot.utils.BotState;
import uz.dataFin.notificationbot.utils.Constant;

@Service
@RequiredArgsConstructor
public class CallBackDataProcessor {
    private final UtilService utilService;
    private final UserService userService;
    private final FeignService feignService;
    private final ReportService reportService;

    public void processor(Update update) {
        String chatId = utilService.getChatIdFromUpdate(update);
        BotState state = userService.getState(chatId);
        Message message = utilService.getMessageFromUpdate(update);
        Integer messageId = utilService.getMessageIdFromUpdate(update);
        String callBackData = (update.hasCallbackQuery()) ? update.getCallbackQuery().getData() : message.getText();

        if (update.hasCallbackQuery()) {
            if (utilService.getErrorDate(update)) {
                state = BotState.SEND_EXCEPTION;
            } else {
                switch (callBackData) {
                    case "agreed" -> state = BotState.SEND_AGREED_MONEY;
                    case "rejected" -> state = BotState.SEND_REJECTED_MONEY;
                    case "pdf" -> state = BotState.EDIT2PDF;
                    case "jpg" -> state = BotState.EDIT2JPG;
                    case "xlsx" -> state = BotState.EDIT2XLSX;
                    case "uz" -> state = BotState.EDIT2UZ;
                    case "ru" -> state = BotState.EDIT2RU;
                    case "kril" -> state = BotState.EDIT2KRIL;
                    case "day" -> state = BotState.SEND_BY_DAY;
                    case "week" -> state = BotState.SEND_BY_WEEK;
                    case "month" -> state = BotState.SEND_BY_LAST_MONTH;
                    case "season" -> state = BotState.SEND_BY_SEASON;
                    case "year" -> state = BotState.SEND_BY_YEAR;
                    case "other" -> state = BotState.SEND_CALENDAR;
                    case "YES" -> state = BotState.SEND_ADS_USERS;
                    case "NOYES", "WITHOUT_CAPTION" -> state = BotState.GET_ADS;
                }
                switch (state) {
                    case START -> {
                        if (!utilService.checkBADate(callBackData) && !utilService.getMonth(update)) {
                            state = BotState.SAVE_START_DATE;
                        } else if (utilService.checkBADate(callBackData)) {
                            state = BotState.EDIT_START_DATE;
                        } else if (utilService.getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        }
                    }
                    case EDIT_START_DATE -> {
                        if (!utilService.checkBADate(callBackData) && !utilService.getMonth(update)) {
                            state = BotState.SAVE_START_DATE;
                        } else if (utilService.getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        }
                    }
                    case SAVE_START_DATE -> {
                        if (!utilService.checkBADate(callBackData) && !utilService.getMonth(update)) {
                            state = BotState.REPORTS;
                        } else if (utilService.checkBADate(callBackData)) {
                            state = BotState.EDIT_END_DATE;
                        } else if (utilService.getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        }
                    }
                    case EDIT_END_DATE -> {
                        if (!utilService.checkBADate(callBackData) && utilService.getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        } else if (!utilService.checkBADate(callBackData) && !utilService.getMonth(update)) {
                            state = BotState.REPORTS;
                        } else if (utilService.getMonth(update)) {
                            state = BotState.SEND_BY_MONTH;
                        }
                    }
                }
                if (callBackData.equals(Constant.ENTER_MARKET_NAME_BTN)) {
                    state = BotState.SEND_MARKET_NAME_TEXT;
                }
            }
        }
        switch (state) {
            case START -> feignService.sendDate(chatId);

            case EDIT2UZ, EDIT2RU, EDIT2KRIL -> feignService.editLanguage(state, chatId, messageId, callBackData);


            case SEND_CALENDAR -> feignService.sendStartDateAsCalendar(message, chatId);

            case EDIT_START_DATE, EDIT_END_DATE -> feignService.editDate(chatId, messageId, callBackData);

            case GET_ADS -> feignService.getAnotherAds(chatId, messageId, callBackData);

            case SEND_ADS_USERS -> feignService.sendUsersAds(chatId, messageId, callBackData);

            case EDIT2XLSX, EDIT2JPG, EDIT2PDF -> feignService.saveTypeFile(state, chatId, messageId, callBackData);

            case SAVE_START_DATE -> feignService.saveStartDate(message, chatId, callBackData);

            case SEND_EXCEPTION -> feignService.sendChoosingDateError(chatId, messageId);

            case REPORTS, SEND_BY_MONTH, SEND_BY_DAY, SEND_BY_WEEK, SEND_BY_LAST_MONTH, SEND_BY_SEASON, SEND_BY_YEAR ->
                    feignService.saveDates(state, chatId, messageId, callBackData);
        }
        if (state != BotState.SEND_CALENDAR)
            if (!state.name().startsWith("SEND_BY"))
                if (state == BotState.SAVE_START_DATE && !reportService.getReportDto(chatId).getTypeReport().equals("DC"))
                    userService.updateUserState(chatId, state);

    }
}
