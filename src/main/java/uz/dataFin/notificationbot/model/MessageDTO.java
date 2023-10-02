package uz.dataFin.notificationbot.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageDTO {
    private String text;
    private String chatId;
    private String sum;
    private String dollar;
    private String bonus;
}
