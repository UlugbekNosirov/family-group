package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uz.dataFin.notificationbot.model.Market;
import uz.dataFin.notificationbot.repository.MarketRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketService {
    private final MarketRepository marketRepository;

    public List<Market> findAll() {
        return marketRepository.findAll(Sort.by("name"));
    }

    public Market getMarket(Long marketId) {
        return marketRepository.findById(marketId).get();
    }

    public Market getMarketByUserName(String userName) {
        return marketRepository.findByUsername(userName);
    }

}
