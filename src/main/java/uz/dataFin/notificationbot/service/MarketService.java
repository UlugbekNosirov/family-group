package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uz.dataFin.notificationbot.model.Market;
import uz.dataFin.notificationbot.repository.MarketRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarketService {
    private final MarketRepository marketRepository;

    public List<Market> findAll() {
        return marketRepository.findAll(Sort.by("name"));
    }

    public Market getMarket(Long marketId) {
        Optional<Market> marketOptional = marketRepository.findById(marketId);
        return marketOptional.orElseGet(Market::new);
    }

    public Market getMarketByUserName(String userName) {
        return marketRepository.findByUsername(userName);
    }

}
