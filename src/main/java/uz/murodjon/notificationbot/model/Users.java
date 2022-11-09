package uz.murodjon.notificationbot.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import uz.murodjon.notificationbot.utils.BotState;
import uz.murodjon.notificationbot.utils.Role;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Users extends BaseModel {

    @Column(unique = true,nullable = false)
    String chatId;

    String firstname;

    String lastname;

    String username;

    @Column(unique = true)
    String phone;

    @Enumerated(value = EnumType.STRING)
    BotState state;

    @ManyToMany
    List<Market> marketList;

    public Users(String chatId, String firstname, String lastname, String username, BotState state) {
        this.chatId = chatId;
        this.firstname = firstname;
        this.lastname = lastname;
        this.username = username;
        this.state = state;
    }
}
