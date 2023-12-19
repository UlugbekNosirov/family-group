package uz.dataFin.notificationbot.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.checkerframework.common.aliasing.qual.Unique;

import javax.persistence.Entity;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateUserStatus extends BaseModel {
    String chatId;
    @Unique
    String documentID;
    String Text;
    String clientId;
    String status;
    String typeDocument;
}