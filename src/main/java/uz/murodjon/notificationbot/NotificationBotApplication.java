package uz.murodjon.notificationbot;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import uz.murodjon.notificationbot.utils.Method;

import static uz.murodjon.notificationbot.utils.Security.*;

@SpringBootApplication
@EnableFeignClients
@RequiredArgsConstructor
public class NotificationBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationBotApplication.class, args);

        System.out.println(restTemplate().getForObject(REQUEST_FULL_PATH + Method.SET_WEB_HOOK +  GLOBAL_URL+ MY_URL, String.class));
    }
    @Bean
    static RestTemplate restTemplate(){
        return new RestTemplate();
    }

}
