package uz.dataFin.notificationbot.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.persistence.Entity;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class WarehouseDTO extends BaseModel {
    String name;
    String id;
}
