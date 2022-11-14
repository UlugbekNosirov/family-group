package uz.murodjon.notificationbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.murodjon.notificationbot.model.Balance;
import uz.murodjon.notificationbot.model.Market;
import uz.murodjon.notificationbot.model.Users;

import java.util.List;

@Repository
public interface BalanceRepository extends JpaRepository<Balance, Long> {
    List<Balance> findByUsers(Users user);
    Balance findByUsersAndMarket(Users user, Market market);
}
