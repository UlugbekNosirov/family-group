package uz.dataFin.notificationbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import uz.dataFin.notificationbot.feign.TelegramFeign;
import uz.dataFin.notificationbot.model.*;
import uz.dataFin.notificationbot.repository.MarketRepository;
import uz.dataFin.notificationbot.repository.UserRepository;
import uz.dataFin.notificationbot.utils.Constant;
import uz.dataFin.notificationbot.utils.Security;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class Api1CService {
    private final TelegramFeign feign;


    public WarehouseDTO[] getWarehouses(String chatId) {
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(Security.LOGIN, Security.PASSWORD, StandardCharsets.UTF_8);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Objects> entity = new HttpEntity<>(null, headers);

            ResponseEntity<byte[]> response = restTemplate
                    .setConnectTimeout(Duration.ofSeconds(60))
                    .setReadTimeout(Duration.ofSeconds(60))
                    .build()
                    .exchange(Constant.REQUEST_URI + "/bot/reports", HttpMethod.POST, entity, byte[].class);
            byte[] responseBody = response.getBody();
            if (responseBody != null) {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(responseBody, WarehouseDTO[].class);
            }
        } catch (Exception e) {
            sendAdminError(LocalDate.now() + " " + chatId + ", " + ", Warehpuse qabul qilib olishda xatolik");
        }
        return new WarehouseDTO[0];
    }

    private void sendAdminError(String text){
        SendMessage sendMessage = new SendMessage("1148134936", text);
        feign.sendMessage(sendMessage);
    }
}

