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
public class Balance extends BaseModel {
    @ManyToOne
    Users users;
    @ManyToOne
    Market market;
    String bonus;
    String sum;
    String dollar;
}
