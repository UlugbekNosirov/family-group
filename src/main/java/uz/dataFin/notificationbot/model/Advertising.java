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
public class Advertising extends BaseModel {

    @Column(unique = true,nullable = false)
    String clientID;

    String filePath;

    Boolean isPhoto;

    String caption;

}
