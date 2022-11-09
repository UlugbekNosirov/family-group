package uz.murodjon.notificationbot.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageDTO {
    private String chatId;
    private String text;
}
