package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import uz.dataFin.notificationbot.model.Users;
import uz.dataFin.notificationbot.model.WarehouseDTO;
import uz.dataFin.notificationbot.repository.UserRepository;
import uz.dataFin.notificationbot.repository.WarehouseRepository;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class WarehouseService {
    private final WarehouseRepository warehouseRepository;

    public void saveOrNONE(WarehouseDTO warehouseDTO){
        Optional<WarehouseDTO> dto = warehouseRepository.findByUniqueID(warehouseDTO.getUniqueID());
        if (dto.isPresent()) {
            dto.get().setName(dto.get().getName());
            warehouseRepository.save(dto.get());
        }else {
            warehouseRepository.save(warehouseDTO);
        }
    }

    public Boolean getByClick(String text){
        Optional<WarehouseDTO> dto = warehouseRepository.findByNameAAndIsBranch(text, Boolean.FALSE);
        return dto.isPresent();
    }

    public Boolean getByClickBranch(String text){
        Optional<WarehouseDTO> dto = warehouseRepository.findByNameAAndNotIsBranch(text, Boolean.TRUE);
        return dto.isPresent();
    }

    public String getByName(String text) {
        Optional<WarehouseDTO> dto = warehouseRepository.findByNameAAndIsBranch(text, Boolean.FALSE);
        return dto.map(WarehouseDTO::getUniqueID).orElse(null);
    }

    public String getWarehouseById(String text) {
        try {
            Optional<WarehouseDTO> dto = warehouseRepository.findWarehouseDTOByUniqueID(text, Boolean.FALSE);
            return dto.map(WarehouseDTO::getName).orElse("-/-/-");
        }catch (Exception e){
            return "-/-/-";
        }
    }

    public String getByNameIsBranch(String text) {
        Optional<WarehouseDTO> dto = warehouseRepository.findByNameAAndNotIsBranch(text, Boolean.TRUE);
        return dto.map(WarehouseDTO::getUniqueID).orElse("-/-/-");
    }

    public String getByWarehouseID(String warehouseID) {
        Optional<WarehouseDTO> dto = warehouseRepository.findByUniqueID(warehouseID);
        return dto.map(WarehouseDTO::getName).orElse("-/-/-");
    }

    public String getBranchNameById(String branchID) {
        Optional<WarehouseDTO> dto = warehouseRepository.findByBranchId(branchID, Boolean.TRUE);
        return dto.map(WarehouseDTO::getName).orElse("-/-/-");
    }
}
