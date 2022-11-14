package uz.murodjon.notificationbot.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import java.util.List;

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
