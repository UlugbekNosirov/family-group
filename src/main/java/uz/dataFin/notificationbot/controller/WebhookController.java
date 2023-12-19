package uz.dataFin.notificationbot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.service.WebhookService;
import static uz.dataFin.notificationbot.utils.Security.MY_URL;

@RestController
@RequestMapping(MY_URL)
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping
    public void getRequests(@RequestBody Update update) {
        webhookService.
                onUpdateToReceive(update);
    }
}
