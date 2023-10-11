package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import uz.dataFin.notificationbot.model.DateDTO;
import uz.dataFin.notificationbot.repository.FileRepository;
import uz.dataFin.notificationbot.utils.Constant;

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
public class FileService {
    private final FileRepository fileRepository;

    public void saveStartDate(String userId, String startDate) {
        DateDTO dateDTO = fileRepository.getDateDTOByClientId(userId);
        if (Objects.nonNull(dateDTO)){
            dateDTO.setStartDate(startDate);
            fileRepository.save(dateDTO);
        }
    }

    public void saveEndDate(String userId, String endDate) {
        DateDTO dto = fileRepository.getDateDTOByClientId(userId);
        if (Objects.nonNull(dto)){
            dto.setEndDate(endDate);
            fileRepository.save(dto);
        }
    }

    public void saveTypeFile(String userId, String typeFile) {
        DateDTO dateDTO = fileRepository.getDateDTOByClientId(userId);
        if (Objects.nonNull(dateDTO)){
            dateDTO.setTypeFile(typeFile);
            fileRepository.save(dateDTO);
        }
    }

    public DateDTO getDateDto(String clientId){
        return fileRepository.getDateDTOByClientId(clientId);
    }

    public File getReports(DateDTO dateDTO) {
        RestTemplateBuilder restTemplate = new RestTemplateBuilder();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth("Админстратор", "2275157", StandardCharsets.UTF_8);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<DateDTO> entity = new HttpEntity<>(dateDTO, headers);

            ResponseEntity<byte[]> response = restTemplate
                    .setConnectTimeout(Duration.ofSeconds(60))
                    .setReadTimeout(Duration.ofSeconds(60))
                    .build()
                    .exchange(Constant.REQUEST_URI +"/bot/reports", HttpMethod.POST, entity, byte[].class);
            Path path= Paths.get("REPORTS");
            path=checkPackage(path);
            Files.write(Paths.get(path.toFile().getAbsolutePath()+"/report."+dateDTO.getTypeFile()), Objects.requireNonNull(response.getBody()));
            return new File(path.toFile().getAbsolutePath()+"/report."+dateDTO.getTypeFile());
        }catch (Exception e){
            System.out.println(LocalDate.now()+" "+dateDTO.getClientId()+", "+", File qabul qilib olishda xatolik, fileService.getReports");
        }
        return null;
    }

    private Path checkPackage(Path file) {
        if (!file.toFile().exists())
            file.toFile().mkdirs();
        return file;
    }

    public void saveReportId(String chatId, int i) {
        DateDTO dateDTO = fileRepository.getDateDTOByClientId(chatId);
        if (Objects.nonNull(dateDTO)){
            dateDTO.setReportId(i);
            dateDTO.setMethodType("REPORT");
            fileRepository.save(dateDTO);
        }
    }

    public void saveMethodType(String chatId, String methodType) {
        DateDTO dateDTO = fileRepository.getDateDTOByClientId(chatId);
        if (Objects.nonNull(dateDTO)){
            dateDTO.setMethodType(methodType);
            fileRepository.save(dateDTO);
        }
    }

    public void saveCode(String chatId, String code) {
        DateDTO dateDTO = fileRepository.getDateDTOByClientId(chatId);
        if (Objects.nonNull(dateDTO)){
            dateDTO.setCode(code);
            fileRepository.save(dateDTO);
        }
    }
}
