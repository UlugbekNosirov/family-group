package uz.dataFin.notificationbot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import uz.dataFin.notificationbot.model.MessageDTO;
import uz.dataFin.notificationbot.model.Users;
import uz.dataFin.notificationbot.service.ApiService;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api")
public class ApiController {
    private final ApiService service;

    @GetMapping("/{username}")
    public ResponseEntity<List<Users>> getUsersByMarket(@PathVariable String username) {
        return ResponseEntity.ok(service.getUsersByMarketUsername(username));
    }

    @PostMapping
    public void sendMessageToUser(@RequestBody MessageDTO messageDTO, @RequestParam("username") String username) {
        service.sendMessageToUser(messageDTO,username);
    }
}
