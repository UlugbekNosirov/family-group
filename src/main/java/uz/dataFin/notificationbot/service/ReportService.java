package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import uz.dataFin.notificationbot.model.DateDTO;
import uz.dataFin.notificationbot.model.ReportDTO;
import uz.dataFin.notificationbot.model.SalesReceipt;
import uz.dataFin.notificationbot.repository.FileRepository;
import uz.dataFin.notificationbot.repository.ReportRepository;
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

    public void saveStartDate(String userId, String startDate) {
        Optional<ReportDTO> reportDTO = reportRepository.findByClientId(userId);
        if (reportDTO.isPresent()){
            reportDTO.get().setStartDate(startDate);
            reportRepository.save(reportDTO.get());
        }
    }

    public void saveEndDate(String userId, String endDate) {
        Optional<ReportDTO> reportDTO = reportRepository.findByClientId(userId);
        if (reportDTO.isPresent()){
            reportDTO.get().setEndDate(endDate);
            reportRepository.save(reportDTO.get());
        }
    }

    public void saveTypeFile(String userId, String typeFile) {
        Optional<ReportDTO> reportDTO = reportRepository.findByClientId(userId);
        if (reportDTO.isPresent()){
            reportDTO.get().setTypeFile(typeFile);
            reportRepository.save(reportDTO.get());
        }
    }



    public ReportDTO getReportDto(String clientId){
        Optional<ReportDTO> reportDTO = reportRepository.findByClientId(clientId);
        return reportDTO.orElse(null);
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
                    .exchange(Constant.REQUEST_URI +"/bot/reports", HttpMethod.POST, entity, byte[].class);
            byte[] responseBody = response.getBody();
            Path path= Paths.get("REPORTS");
            path=utilService.checkPackage(path);
            Files.write(Paths.get(path.toFile().getAbsolutePath()+"/report."+dateDTO.getTypeFile()), Objects.requireNonNull(response.getBody()));
            return new File(path.toFile().getAbsolutePath()+"/report."+dateDTO.getTypeFile());
        }catch (Exception e){
            System.out.println(LocalDate.now()+" "+dateDTO.getClientId()+", "+", File qabul qilib olishda xatolik, fileService.getReports");
        }
        return null;
    }



    public void saveNew(String chatId) {
        Optional<ReportDTO> reportDTO = reportRepository.findByClientId(chatId);
        if (reportDTO.isEmpty()){
            reportRepository.save(new ReportDTO(chatId, LocalDate.now().toString(), LocalDate.now().toString(), "pdf"));
        }
    }

}
