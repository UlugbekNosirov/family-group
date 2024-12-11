package uz.dataFin.notificationbot.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import uz.dataFin.notificationbot.payload.Result;
import uz.dataFin.notificationbot.utils.Method;

import static uz.dataFin.notificationbot.utils.Security.REQUEST_FULL_PATH;


@FeignClient(url = REQUEST_FULL_PATH, name = "TelegramFeign")
public interface TelegramFeign extends Method {

    @PostMapping(ANSWER_INLINE_QUERY)
    Result answerInlineQuery(@RequestBody AnswerInlineQuery answerInlineQuery);

    @PostMapping(SEND_MESSAGE)
    Result sendMessage(@RequestBody SendMessage sendMessage);

    @PostMapping(EDIT_MESSAGE_TEXT)
    Result editMessageText(@RequestBody EditMessageText editMessageText);

    @PostMapping(SEND_DOCUMENT)
    Result SendDocument(@RequestBody SendDocument sendDocument);

    @PostMapping(SEND_LOCATION)
    Result sendLocation(@RequestBody SendLocation sendLocation);

    @PostMapping(DELETE_MESSAGE)
    Result deleteMessage(@RequestBody DeleteMessage deleteMessage);

    @PostMapping(EDIT_MESSAGE_REPLY_MARKUP)
    Result editMessageReplyMarkup(@RequestBody EditMessageReplyMarkup editMessageReplyMarkup);

}
