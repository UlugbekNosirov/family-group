package uz.murodjon.notificationbot.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import uz.murodjon.notificationbot.payload.Result;
import uz.murodjon.notificationbot.utils.Method;

import static uz.murodjon.notificationbot.utils.Security.REQUEST_FULL_PATH;


@FeignClient(url = REQUEST_FULL_PATH, name = "TelegramFeign")
public interface TelegramFeign extends Method {


    @PostMapping(SEND_MESSAGE)
    Result sendMessage(@RequestBody SendMessage sendMessage);


    @PostMapping(EDIT_MESSAGE_TEXT)
    Result editMessageText(@RequestBody EditMessageText editMessageText);

    @PostMapping(SEND_LOCATION)
    Result sendLocation(@RequestBody SendLocation sendLocation);


}
