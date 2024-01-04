package uz.dataFin.notificationbot.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.persistence.Entity;
import java.util.ArrayList;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SearchDTO extends BaseModel{

    String chatId;

    String name;

    String code;

    String uniqueID;

    String branchID;

    String search;

    Integer page;

    Integer pageSize;

    public SearchDTO(String chatId, String search, Integer page, Integer pageSize){
        this.chatId = chatId;
        this.search = search;
        this.page   = page;
        this.pageSize = pageSize;
    }

    public SearchDTO(String chatId, String branchID, String search, Integer page, Integer pageSize){
        this.chatId = chatId;
        this.branchID = branchID;
        this.search = search;
        this.page   = page;
        this.pageSize = pageSize;
    }


}
