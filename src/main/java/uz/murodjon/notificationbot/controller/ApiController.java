package uz.murodjon.notificationbot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.murodjon.notificationbot.model.MessageDTO;
import uz.murodjon.notificationbot.model.Users;
import uz.murodjon.notificationbot.service.ApiService;

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
