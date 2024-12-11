package uz.dataFin.notificationbot.model.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import uz.dataFin.notificationbot.utils.BotState;

@AllArgsConstructor
@Data
public class UserDTO {
    BotState state;
}
