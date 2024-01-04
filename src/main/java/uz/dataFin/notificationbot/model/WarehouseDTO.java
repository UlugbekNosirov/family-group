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
public class WarehouseDTO extends BaseModel{

    String name;

    String uniqueID;

    Integer code;

    Boolean isBranch;


    public WarehouseDTO(String name, String uniqueID, Boolean isBranch){
        this.name = name;
        this.uniqueID = uniqueID;
        this.isBranch = isBranch;
    }


}
