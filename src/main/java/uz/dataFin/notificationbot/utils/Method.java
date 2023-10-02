package uz.dataFin.notificationbot.utils;

public interface Method {
    String SET_WEB_HOOK = "/setWebhook?url=";
    String SEND_MESSAGE = "/SendMessage";
    String SEND_PHOTO = "/SendPhoto";
    String EDIT_MESSAGE_TEXT = "/editMessageText";
    String SEND_LOCATION = "/sendLocation";
    String SEND_DOCUMENT = "/sendDocument";
    String DELETE_MESSAGE = "/deleteMessage";

    String EDIT_MESSAGE_REPLY_MARKUP = "/editMessageReplyMarkup";
}
