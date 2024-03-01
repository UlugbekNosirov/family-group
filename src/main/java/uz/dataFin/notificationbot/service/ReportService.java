package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import uz.dataFin.notificationbot.model.*;
import uz.dataFin.notificationbot.repository.FileRepository;
import uz.dataFin.notificationbot.repository.ReportRepository;
import uz.dataFin.notificationbot.repository.SearchRepository;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final UtilService utilService;
    private final SearchRepository searchRepository;
    private final Api1CService api1CService;
    private final UserService userService;

    public void saveStartDate(String userId, String startDate) {
        Optional<ReportDTO> reportDTO = reportRepository.findByClientId(userId);
        if (reportDTO.isPresent()) {
            reportDTO.get().setStartDate(startDate);
            reportRepository.save(reportDTO.get());
        }
    }

    public void savePeriod(String userId, String startDate) {
        Optional<ReportDTO> reportDTO = reportRepository.findByClientId(userId);
        if (reportDTO.isPresent()) {
            reportDTO.get().setPeriod(startDate);
            reportRepository.save(reportDTO.get());
        }
    }

    public SearchDTO saveSearch(String userId, String branchId, String search) {
        Optional<SearchDTO> searchDTO = searchRepository.findByChatId(userId);
        if (searchDTO.isPresent()) {
            searchDTO.get().setSearch(search);
            searchDTO.get().setBranchID(branchId);
            searchDTO.get().setPage(1);
            searchDTO.get().setPageSize(10);
            return searchRepository.save(searchDTO.get());
        } else {
            return searchRepository.save(new SearchDTO(userId, search, 1, 10));
        }
    }

    public void saveEndDate(String userId, String endDate) {
        Optional<ReportDTO> reportDTO = reportRepository.findByClientId(userId);
        if (reportDTO.isPresent()) {
            reportDTO.get().setEndDate(endDate);
            reportRepository.save(reportDTO.get());
        }
    }

    public void saveTypeFile(String userId, String typeFile) {
        Optional<ReportDTO> reportDTO = reportRepository.findByClientId(userId);
        if (reportDTO.isPresent()) {
            reportDTO.get().setTypeFile(typeFile);
            reportRepository.save(reportDTO.get());
        }
    }


    public ReportDTO getReportDto(String clientId) {
        Optional<ReportDTO> reportDTO = reportRepository.findByClientId(clientId);
        return reportDTO.orElseGet(() -> reportRepository.save(new ReportDTO(clientId, "", "", "","", "", "", "", "", "", 0, "", "")));
    }


    public File getReports(DateDTO dateDTO) {
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
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
            byte[] responseBody = response.getBody();
            Path path = Paths.get("REPORTS");
            path = utilService.checkPackage(path);
            Files.write(Paths.get(path.toFile().getAbsolutePath() + "/report." + dateDTO.getTypeFile()), Objects.requireNonNull(response.getBody()));
            return new File(path.toFile().getAbsolutePath() + "/report." + dateDTO.getTypeFile());
        } catch (Exception e) {
            System.out.println(LocalDate.now() + " " + dateDTO.getClientId() + ", " + ", File qabul qilib olishda xatolik, fileService.getReports");
        }
        return null;
    }


    public void saveNew(String chatId, String typeReport) {
        Optional<ReportDTO> reportDTO = reportRepository.findByClientId(chatId);
        if (reportDTO.isEmpty()) {
            reportRepository.save(new ReportDTO(chatId, LocalDate.now().toString(), LocalDate.now().toString(), "pdf", typeReport));
        }else {
            Optional<ReportDTO> reportDTO1 = reportRepository.findByClientId(chatId);
            reportDTO1.get().setTypeReport(typeReport);
            reportRepository.save(reportDTO1.get());
        }
        userService.updateUserState(chatId, BotState.START);
    }

    public void saveWareHouseId(String chatId, String uniqueId) {
        Optional<ReportDTO> reportDTO = reportRepository.findByClientId(chatId);
        if (reportDTO.isPresent()) {
            reportDTO.get().setWarehouseID(uniqueId);
            reportRepository.save(reportDTO.get());
        }
    }

    public ReportDTO saveProductId(BotState state, String chatID, String text) {
        SearchDTO searchDTO = new SearchDTO();
        searchDTO.setSearch(text);
        searchDTO.setChatId(chatID);
        searchDTO.setPage(1);
        searchDTO.setPageSize(10);
        SearchDTO[] productGroup = api1CService.getProductGroup(state, searchDTO);
        Optional<ReportDTO> reportDTO = reportRepository.findByClientId(chatID);
        if (reportDTO.isPresent()) {
            try {
                reportDTO.get().setProductID(productGroup[0].getUniqueID());
            } catch (Exception e) {
                reportDTO.get().setProductID("");
            }
            return reportRepository.save(reportDTO.get());
        }
        return null;
    }

    public ReportDTO saveClientId(String chatID, String text) {

        SearchDTO[] productGroup = api1CService.getProductGroup(BotState.GET_CONTRACTORS, searchRepository.findByChatId(chatID).get());

        Optional<ReportDTO> reportDTO = reportRepository.findByClientId(chatID);

        if (reportDTO.isPresent()) {
            try {

                if (text.equals(utilService.getTextByLanguage(chatID, Constant.NO_FILTR_PRODUCT))) {reportDTO.get().setContractorID("");
                }   else reportDTO.get().setContractorID(productGroup[0].getUniqueID());}

            catch (Exception e) {

                reportDTO.get().setContractorID("");
            }
            return reportRepository.save(reportDTO.get());
        }
        return null;
    }

    public ReportDTO saveCashID(String chatId, String text) {
        SearchDTO searchDTO = new SearchDTO();
        searchDTO.setSearch(text);
        searchDTO.setChatId(chatId);
        SearchDTO[] productGroup = api1CService.getCashBox(searchDTO, "cashbox");
        Optional<ReportDTO> reportDTO = reportRepository.findByClientId(chatId);
        if (reportDTO.isPresent()) {
            try {
                reportDTO.get().setCashboxID(productGroup[0].getUniqueID());
            } catch (Exception e) {
                reportDTO.get().setCashboxID("");
            }
            return reportRepository.save(reportDTO.get());
        }
        return null;
    }

    public void saveBranchID(String chatId, String text) {
        Optional<ReportDTO> reportDTO = reportRepository.findByClientId(chatId);
        if (reportDTO.isPresent()) {
            try {
                reportDTO.get().setBranchID(text);
            } catch (Exception e) {
                reportDTO.get().setBranchID("");
            }
            reportRepository.save(reportDTO.get());
        }
    }

    public void saveTypeContractor(String chatId, int text) {
        Optional<ReportDTO> reportDTO = reportRepository.findByClientId(chatId);
        if (reportDTO.isPresent()) {
            reportDTO.get().setTypeContractor(text);
            reportRepository.save(reportDTO.get());
        }
    }

    public SearchDTO getTankTankID(String chatId, String text) {
        SearchDTO[] productGroup = api1CService.getProductGroup(BotState.GET_TANKS, new SearchDTO(chatId, text, 1, 200));
        return productGroup[0];
    }

    public void setReportType(String chatId, String callBackData) {
        try {
            if (callBackData.equals(utilService.getTextByLanguage(chatId, Constant.AKT_SVERKA)))
                saveNew(chatId, "AKT_SVERKA");
            else if (callBackData.equals(utilService.getTextByLanguage(chatId, Constant.AKT_SVERKA_TOVAR)))
                saveNew(chatId, "AKT_SVERKA_TOVAR");
            else if (callBackData.equals(utilService.getTextByLanguage(chatId, Constant.WAREHOUSE_BTN)))
                saveNew(chatId, "WAREHOUSE");
            else if (callBackData.equals(utilService.getTextByLanguage(chatId, Constant.TRADE)))
                saveNew(chatId, "TRADE");
            else if (callBackData.equals(utilService.getTextByLanguage(chatId, Constant.CASH_BOX)))
                saveNew(chatId, "CASH_BOX");
            else if (callBackData.equals(utilService.getTextByLanguage(chatId, Constant.COST)))
                saveNew(chatId, "COST");
            else if (callBackData.equals(utilService.getTextByLanguage(chatId, Constant.ACCOUNT_DEBT)))
                saveNew(chatId, "DC");
            else if (callBackData.equals(utilService.getTextByLanguage(chatId, Constant.TRADE_ALKAN)))
                saveNew(chatId, "TRADE_ALKAN");
            else if (callBackData.equals(utilService.getTextByLanguage(chatId, Constant.CASH_BOX_SMENA)))
                saveNew(chatId, "CASH_BOX_SMENA");
            else if (callBackData.equals(utilService.getTextByLanguage(chatId, Constant.SETTINGS)))
                saveNew(chatId, "SETTINGS");

        }catch (Exception e){
            System.out.println("Exception e");
        }
    }

    public void saveProductIdNONE(String chatId) {
        Optional<ReportDTO> reportDTO = reportRepository.findByClientId(chatId);
        if (reportDTO.isPresent()){
            reportDTO.get().setProductID("NONE");
            reportRepository.save(reportDTO.get());
        }
    }
}
