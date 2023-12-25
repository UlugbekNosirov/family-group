package uz.dataFin.notificationbot.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ReportDTO extends BaseModel {
    String clientId;
    String warehouseID;
    String startDate;
    String endDate;
    String productID;
    String typeFile;
    public ReportDTO(String clientId, String startDate, String endDate, String typeFile){
        this.clientId  = clientId;
        this.startDate = startDate;
        this.endDate   = endDate;
        this.typeFile  = typeFile;
    }
}
