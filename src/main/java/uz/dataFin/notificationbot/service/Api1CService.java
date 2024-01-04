package uz.dataFin.notificationbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import uz.dataFin.notificationbot.feign.TelegramFeign;
import uz.dataFin.notificationbot.model.*;
import uz.dataFin.notificationbot.repository.MarketRepository;
import uz.dataFin.notificationbot.repository.UserRepository;
import uz.dataFin.notificationbot.utils.BotState;
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
    private final UtilService utilService;
    private final UserService userService;


    public WarehouseDTO[] getWarehouses(String chatId, String uri) {
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
                    .exchange(Constant.REQUEST_URI + "/bot/"+uri, HttpMethod.POST, entity, byte[].class);
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

    public SearchDTO[] getProductGroup(BotState state, SearchDTO searchDTO) {
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        String FULL_URI = "";
        if (state == BotState.GET_PRODUCT_IN_API || state == BotState.GET_REPORT_WAREHOUSE_BY_PRODUCT){
            FULL_URI = Constant.REQUEST_URI + "/bot/product";
        }else if (state == BotState.GET_CONTRACTOR_IN_API) {
            FULL_URI = Constant.REQUEST_URI + "/bot/contractor";
        }else {
            FULL_URI = Constant.REQUEST_URI + "/bot/product/group";

        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(Security.LOGIN, Security.PASSWORD, StandardCharsets.UTF_8);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<SearchDTO> entity = new HttpEntity<>(searchDTO, headers);

            ResponseEntity<byte[]> response = restTemplate
                    .setConnectTimeout(Duration.ofSeconds(60))
                    .setReadTimeout(Duration.ofSeconds(60))
                    .build()
                    .exchange(FULL_URI, HttpMethod.POST, entity, byte[].class);
            byte[] responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(responseBody, SearchDTO[].class);
            } else {
                System.err.println("Request failed with status code: " + response.getStatusCodeValue());
            }
        } catch (Exception e) {
            sendAdminError(LocalDate.now() + " " + searchDTO.getChatId() + ", Warehpuse qabul qilib olishda xatolik");
        }
        return new SearchDTO[0];
    }

    public SearchDTO[] getCashBox(SearchDTO searchDTO, String uri) {
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        String FULL_URI = Constant.REQUEST_URI + "/bot/"+uri;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(Security.LOGIN, Security.PASSWORD, StandardCharsets.UTF_8);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<SearchDTO> entity = new HttpEntity<>(searchDTO, headers);

            ResponseEntity<byte[]> response = restTemplate
                    .setConnectTimeout(Duration.ofSeconds(60))
                    .setReadTimeout(Duration.ofSeconds(60))
                    .build()
                    .exchange(FULL_URI, HttpMethod.POST, entity, byte[].class);
            byte[] responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(responseBody, SearchDTO[].class);
            } else {
                System.err.println("Request failed with status code: " + response.getStatusCodeValue());
            }
        } catch (Exception e) {
            sendAdminError(LocalDate.now() + " " + searchDTO.getChatId() + ", Warehpuse qabul qilib olishda xatolik");
        }
        return new SearchDTO[0];
    }

    public Boolean haveProductGroup(BotState state, SearchDTO searchDTO){
        SearchDTO[] productGroup = getProductGroup(state, searchDTO);
        String search = searchDTO.getSearch().trim();
        for (SearchDTO dto : productGroup) {
            String name =dto.getName().trim();
            if (name.equals(search))
                return Boolean.TRUE;
        }
        return false;
    }

    public File getReports(ReportDTO reportDTO) {
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        String fullUri = Constant.REQUEST_URI + "/bot/report";
        if (reportDTO.getTypeReport().equals("WAREHOUSE")) {
            fullUri = fullUri + "/warehouse";
        } else if (reportDTO.getTypeReport().equals("TRADE")) {
            fullUri = fullUri + "/sale";
        } else if (reportDTO.getTypeReport().equals("CASH_BOX")) {
            fullUri = fullUri + "/cashbox";
        } else if (reportDTO.getTypeReport().equals(utilService.getTextByLanguage(reportDTO.getClientId(), Constant.AKT_SVERKA))) {
            if (userService.getRole(reportDTO.getClientId()).equals("Contractor")){
                return getReportContractor(reportDTO);
            }else {
                fullUri = fullUri + "/sverka";
            }
        } else if (reportDTO.getTypeReport().equals(utilService.getTextByLanguage(reportDTO.getClientId(), Constant.AKT_SVERKA_TOVAR))) {
            if (userService.getRole(reportDTO.getClientId()).equals("Contractor")){
                return getReportContractor(reportDTO);
            }else {
                fullUri = fullUri + "/sverka/product";
            }
        } else if (reportDTO.getTypeReport().equals("DC")) {
            fullUri = fullUri + "/dc";
        }else if (reportDTO.getTypeReport().equals("COST")) {
            fullUri = fullUri + "/expense";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(Security.LOGIN, Security.PASSWORD, StandardCharsets.UTF_8);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<ReportDTO> entity = new HttpEntity<>(reportDTO, headers);

            ResponseEntity<byte[]> response = restTemplate
                    .setConnectTimeout(Duration.ofSeconds(60))
                    .setReadTimeout(Duration.ofSeconds(60))
                    .build()
                    .exchange(fullUri, HttpMethod.POST, entity, byte[].class);
            Path path= Paths.get("REPORTS");
            path=utilService.checkPackage(path);
            Files.write(Paths.get(path.toFile().getAbsolutePath()+"/report."+reportDTO.getTypeFile()), Objects.requireNonNull(response.getBody()));
            return new File(path.toFile().getAbsolutePath()+"/report."+reportDTO.getTypeFile());
        }catch (Exception e){
            System.out.println(LocalDate.now()+" "+reportDTO.getClientId()+", "+", File qabul qilib olishda xatolik, fileService.getReports");
        }
        return null;
    }

    private File getReportContractor(ReportDTO reportDTO) {
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        DateDTO dateDTO = DateDTO.builder()
                .methodType("REPORT")
                .startDate(reportDTO.getStartDate())
                .endDate(reportDTO.getEndDate())
                .contractorId(reportDTO.getClientId())
                .typeFile(reportDTO.getTypeFile())
                .reportId((reportDTO.getTypeReport().equals(utilService.getTextByLanguage(reportDTO.getClientId(), Constant.AKT_SVERKA))) ? 1 : 2).build();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(Security.LOGIN, Security.PASSWORD, StandardCharsets.UTF_8);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<DateDTO> entity = new HttpEntity<>(dateDTO, headers);

            ResponseEntity<byte[]> response = restTemplate
                    .setConnectTimeout(Duration.ofSeconds(60))
                    .setReadTimeout(Duration.ofSeconds(60))
                    .build()
                    .exchange(Constant.REQUEST_URI + "/bot/reports", HttpMethod.POST, entity, byte[].class);
            Path path = Paths.get("REPORTS");
            path = utilService.checkPackage(path);
            Files.write(Paths.get(path.toFile().getAbsolutePath() + "/report." + reportDTO.getTypeFile()), Objects.requireNonNull(response.getBody()));
            return new File(path.toFile().getAbsolutePath() + "/report." + reportDTO.getTypeFile());
        } catch (Exception e) {
            System.out.println(LocalDate.now() + " " + reportDTO.getClientId() + ", " + ", File qabul qilib olishda xatolik, fileService.getReports");
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
        } catch (Exception e) {
            System.out.println(LocalDate.now() + " " + user.getChatId() + ", " + ", User yaratishda xatolik");
        }
    }
}

