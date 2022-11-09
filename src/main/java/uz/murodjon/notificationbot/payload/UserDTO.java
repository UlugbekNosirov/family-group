package uz.murodjon.notificationbot.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import uz.murodjon.notificationbot.utils.BotState;
@AllArgsConstructor
@Data
public class UserDTO {
    BotState state;
}
