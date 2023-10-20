package uz.dataFin.notificationbot.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import uz.dataFin.notificationbot.utils.BotState;

import javax.persistence.*;
import java.util.ArrayList;
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

    String Role;

    String language;

    @Column(unique = true)
    String phone;

    @Enumerated(value = EnumType.STRING)
    BotState state;

    @ManyToMany
    List<Market> marketList = new ArrayList<>();

    public Users(String chatId, String firstname, String lastname, String username, String role, String language) {
        this.chatId = chatId;
        this.firstname = firstname;
        this.lastname = lastname;
        this.username = username;
        this.Role = role;
        this.language = language;
    }
}
