package uz.murodjon.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.murodjon.notificationbot.model.Balance;
import uz.murodjon.notificationbot.model.Market;
import uz.murodjon.notificationbot.model.MessageDTO;
import uz.murodjon.notificationbot.model.Users;
import uz.murodjon.notificationbot.repository.BalanceRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BalanceService {
    private final BalanceRepository balanceRepository;

    public List<Balance> findByUsers(Users users) {
        return balanceRepository.findByUsers(users);
    }

    public void saveBalance(MessageDTO messageDTO, Market market, Users user){
        Balance balance = balanceRepository.findByUsersAndMarket(user, market);
        if (balance == null){
            balance = new Balance();
            balance.setUsers(user);
            balance.setMarket(market);
        }
        if (!messageDTO.getBonus().equals("-1")){
            balance.setBonus(messageDTO.getBonus());
        }else{
            balance.setSum(messageDTO.getSum());
            balance.setDollar(messageDTO.getDollar());
        }
        balanceRepository.save(balance);
    }
}
