package uz.dataFin.notificationbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.dataFin.notificationbot.model.Balance;
import uz.dataFin.notificationbot.model.Market;
import uz.dataFin.notificationbot.model.MessageDTO;
import uz.dataFin.notificationbot.model.Users;
import uz.dataFin.notificationbot.repository.BalanceRepository;

@Service
@RequiredArgsConstructor
public class BalanceService {
    private final BalanceRepository balanceRepository;

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
