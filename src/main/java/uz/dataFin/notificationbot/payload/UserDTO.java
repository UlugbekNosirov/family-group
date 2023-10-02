package uz.dataFin.notificationbot.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import uz.dataFin.notificationbot.utils.BotState;

@AllArgsConstructor
@Data
public class UserDTO {
    BotState state;
}
