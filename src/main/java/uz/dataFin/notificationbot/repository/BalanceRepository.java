package uz.dataFin.notificationbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.dataFin.notificationbot.model.Balance;
import uz.dataFin.notificationbot.model.Market;
import uz.dataFin.notificationbot.model.Users;

import java.util.List;

@Repository
public interface BalanceRepository extends JpaRepository<Balance, Long> {
    List<Balance> findByUsers(Users user);
    Balance findByUsersAndMarket(Users user, Market market);
}
