package uz.dataFin.notificationbot.repository;

import org.hibernate.query.NativeQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.dataFin.notificationbot.model.SalesReceipt;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalesReceiptRepository extends JpaRepository<SalesReceipt, Long> {
    @Query(nativeQuery = true, value = "select * from sales_receipt i where  i.active is false")
    List<SalesReceipt> getAllActiveIsFalse();

    @Query(nativeQuery = true, value = "select * from sales_receipt i where i.url=?1")
    Optional<SalesReceipt> findByUrl(String url);

}
