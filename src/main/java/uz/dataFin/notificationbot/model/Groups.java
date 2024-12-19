package uz.dataFin.notificationbot.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.persistence.Column;
import javax.persistence.Entity;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Groups extends BaseModel {

    @Column(unique = true,nullable = false)
    String groupId;

    String groupName;

    String invitationLink;

    String userName;

    String addedId;

    String status;

}
