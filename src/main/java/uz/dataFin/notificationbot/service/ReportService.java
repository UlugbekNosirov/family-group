package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.dataFin.notificationbot.model.*;

import uz.dataFin.notificationbot.repository.ReportRepository;

import uz.dataFin.notificationbot.utils.BotState;
import uz.dataFin.notificationbot.utils.Constant;
import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final UtilService utilService;
    private final UserService userService;

    public void saveStartDate(String userId, String startDate) {
        Optional<Report> reportDTO = reportRepository.findByClientId(userId);
        if (reportDTO.isPresent()) {
            reportDTO.get().setStartDate(startDate);
            reportRepository.save(reportDTO.get());
        }
    }

    public void saveEndDate(String userId, String endDate) {
        Optional<Report> reportDTO = reportRepository.findByClientId(userId);
        if (reportDTO.isPresent()) {
            reportDTO.get().setEndDate(endDate);
            reportRepository.save(reportDTO.get());
        }
    }

    public void saveTypeFile(String userId, String typeFile) {
        Optional<Report> reportDTO = reportRepository.findByClientId(userId);
        if (reportDTO.isPresent()) {
            reportDTO.get().setTypeFile(typeFile);
            reportRepository.save(reportDTO.get());
        }
    }


    public Report getReportDto(String clientId) {
        Optional<Report> reportDTO = reportRepository.findByClientId(clientId);
        return reportDTO.orElseGet(() -> reportRepository.save(new Report(clientId, "", "", "","", "", "", "", "", "", "", 0, "", "", 1)));
    }


    public void saveNew(String chatId, String typeReport) {
        Optional<Report> reportDTO = reportRepository.findByClientId(chatId);
        if (reportDTO.isEmpty()) {
            reportRepository.save(new Report(chatId, LocalDate.now().toString(), LocalDate.now().toString(), "pdf", typeReport));
        }else {
            reportDTO.get().setTypeReport(typeReport);
            reportRepository.save(reportDTO.get());
        }
        userService.updateUserState(chatId, BotState.START);
    }

    public void setReportType(String chatId, String callBackData) {
        try {
            if (callBackData.equals(utilService.getTextByLanguage(chatId, Constant.AKT_SVERKA)))
                saveNew(chatId, "AKT_SVERKA");
            else if (callBackData.equals(utilService.getTextByLanguage(chatId, Constant.AKT_SVERKA_TOVAR)))
                saveNew(chatId, "AKT_SVERKA_TOVAR");
            else if (callBackData.equals(utilService.getTextByLanguage(chatId, Constant.BALANCE)))
                saveNew(chatId, "BALANCE");
            else if (callBackData.equals("/reklama"))
                saveNew(chatId, "ADS");
            else if (callBackData.equals(utilService.getTextByLanguage(chatId, Constant.SETTINGS)))
                saveNew(chatId, "SETTINGS");

        }catch (Exception e){
            System.out.println("Exception e");
        }
    }
}
