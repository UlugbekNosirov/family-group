package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import uz.dataFin.notificationbot.model.*;
import uz.dataFin.notificationbot.utils.Constant;
import uz.dataFin.notificationbot.utils.Security;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class Api1CService {
    private final UtilService utilService;
    private final UserService userService;


    public File getReports(Report report) {
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        report.setReportId(report.getTypeReport().equals("AKT_SVERKA") ? 1 : 2);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(Security.LOGIN, Security.PASSWORD, StandardCharsets.UTF_8);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Report> entity = new HttpEntity<>(report, headers);

            ResponseEntity<byte[]> response = restTemplate
                    .setConnectTimeout(Duration.ofSeconds(60))
                    .setReadTimeout(Duration.ofSeconds(60))
                    .build()
                    .exchange(Constant.REQUEST_URI + "/bot/reports", HttpMethod.POST, entity, byte[].class);
            Path path = Paths.get("REPORTS");
            path = utilService.checkPackage(path);
            Files.write(Paths.get(path.toFile().getAbsolutePath() + "/report." + report.getTypeFile()), Objects.requireNonNull(response.getBody()));
            return new File(path.toFile().getAbsolutePath() + "/report." + report.getTypeFile());
        } catch (Exception e) {
            System.out.println(LocalDate.now() + " " + report.getClientId() + ", " + ", File qabul qilib olishda xatolik, fileService.getReports");
        }
        return null;
    }

    public void saveClient(Users user) {
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(Security.LOGIN, Security.PASSWORD, StandardCharsets.UTF_8);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Users> entity = new HttpEntity<>(user, headers);

            ResponseEntity<byte[]> response = restTemplate
                    .setConnectTimeout(Duration.ofSeconds(60))
                    .setReadTimeout(Duration.ofSeconds(60))
                    .build()
                    .exchange(Constant.REQUEST_URI + "/bot/save/client", HttpMethod.POST, entity, byte[].class);
            System.out.println(response);
        } catch (Exception e) {
            System.out.println(e + " " + user.getChatId() + ", " + ", User yaratishda xatolik");
        }
    }

    public String getBalance(String chatId) {
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        String methodType = "BALANCE";
        if (userService.getRoleInURL(chatId).equals("Admin")) methodType = "EMPLOYEE";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(Security.LOGIN, Security.PASSWORD, StandardCharsets.UTF_8);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Report> entity = new HttpEntity<>(new Report(chatId, methodType), headers);

            ResponseEntity<byte[]> response = restTemplate
                    .setConnectTimeout(Duration.ofSeconds(15))
                    .setReadTimeout(Duration.ofSeconds(60))
                    .build()
                    .exchange(Constant.REQUEST_URI + "/bot/reports", HttpMethod.POST, entity, byte[].class);
            byte[] responseBody = response.getBody();
            if (responseBody != null) {
                return new String(responseBody, StandardCharsets.UTF_8);

            } else {
                return utilService.getTextByLanguage(chatId, Constant.NO_BALANCE);
            }
        } catch (Exception e) {
            System.out.println("\n\n\nBalance olishda xatolik");
            return "NO BALANCE";
        }
    }
}

