package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
//import uz.dataFin.notificationbot.model.MessageDTO;
import uz.dataFin.notificationbot.model.MessageDTO;
import uz.dataFin.notificationbot.model.SalesReceipt;
import uz.dataFin.notificationbot.model.Users;
import uz.dataFin.notificationbot.repository.MarketRepository;
import uz.dataFin.notificationbot.repository.SalesReceiptRepository;
import uz.dataFin.notificationbot.repository.UserRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SalesReceiptService {
    private final SalesReceiptRepository salesReceiptRepository;

//    public void saveUrlById(MessageDTO messageDTO) {
//        Optional<SalesReceipt> byUrl = salesReceiptRepository.findByUrl(messageDTO.getUrl());
//        if (byUrl.isPresent()) {
//            if (byUrl.get().isActive()) {
//                SalesReceipt sale = byUrl.get();
//                sale.setClientId(messageDTO.getChatId());
//                sale.setActive(false);
//                salesReceiptRepository.save(sale);
//            }
//        } else {
//            SalesReceipt sale = new SalesReceipt();
//            sale.setClientId(messageDTO.getChatId());
//            sale.setUrl(messageDTO.getUrl());
//            sale.setMethodType("SALESRECEIPT");
//            sale.setActive(false);
//            salesReceiptRepository.save(sale);
//        }
//    }

    public List<SalesReceipt> getNonActiveUsers() {
        return salesReceiptRepository.getAllActiveIsFalse();
    }

    public void changeToActive(SalesReceipt salesReceipt) {
        Optional<SalesReceipt> sale = salesReceiptRepository.findByUrl(salesReceipt.getUrl());
        if (sale.isPresent()) {
            sale.get().setActive(true);
            salesReceiptRepository.save(sale.get());
        }
    }
}
