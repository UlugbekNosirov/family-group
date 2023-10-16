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
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class SalesReceipt extends BaseModel {
    String clientId;
    String methodType;
    @Unique
    String url;
}
