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
public class Report extends BaseModel {
    String clientId;
    String warehouseID;
    String contractorID;
    String tankID;
    String period;
    String startDate;
    String endDate;
    String productID;
    String branchID;
    String methodType;
    String typeFile;
    Integer typeContractor;
    String cashboxID;
    String typeReport;
    Integer reportId;

    public Report(String clientId, String methodType){
        this.clientId = clientId;
        this.methodType = methodType;
    }
    public Report(String clientId, String startDate, String endDate, String typeFile, String typeReport){
        this.clientId  = clientId;
        this.startDate = startDate;
        this.endDate   = endDate;
        this.typeFile  = typeFile;
        this.typeReport  = typeReport;
    }

    public Report(String warehouseID){
        this.warehouseID = warehouseID;
    }
}
