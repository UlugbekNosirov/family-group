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
    String contractorID;
    String period;
    String startDate;
    String endDate;
    String productID;
    String branchID;
    String typeFile;
    Integer typeContractor;
    String cashboxID;
    String typeReport;
    public ReportDTO(String clientId, String startDate, String endDate, String typeFile, String typeReport){
        this.clientId  = clientId;
        this.startDate = startDate;
        this.endDate   = endDate;
        this.typeFile  = typeFile;
        this.typeFile  = typeReport;
    }

    public ReportDTO(String warehouseID){
        this.warehouseID = warehouseID;
    }
}
