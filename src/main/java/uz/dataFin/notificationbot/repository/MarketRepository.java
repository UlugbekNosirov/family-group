package uz.dataFin.notificationbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.dataFin.notificationbot.model.Market;

import java.util.List;

@Repository
public interface MarketRepository extends JpaRepository<Market, Long> {
    @Query(nativeQuery = true, value = "select users_id from users_market_list i where i.users_id =?1 and i.market_list_id =?2")
    Long getUserMarketId(Long usersId, Long marketListId);

    @Query(nativeQuery = true, value = "select users_id from users_market_list i where  i.market_list_id =?1")
    List<Long> getUsersByMarketId(Long marketId);

    @Query(nativeQuery = true, value = "select id from market i where  i.username =?1")
    Long getMarketIdByUsername(String username);


    @Query(nativeQuery = true, value = "select i.id from where market i.username=''")
    Long getMarketId();

    Market getMarketByUsernameIsNull();

    Market findByUsername(String userName);
}
