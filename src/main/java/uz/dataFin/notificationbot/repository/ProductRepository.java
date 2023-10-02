package uz.dataFin.notificationbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.dataFin.notificationbot.model.Products;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Products, Long> {
    @Query(nativeQuery = true, value = "select * from public.products where client_id =?1 and code = ?2 order by id asc;")
    Optional<Products> getByCodeAndClientId(String clientId, String code);

    @Query(nativeQuery = true, value = "delete from products where id = ?1 ")
    void deleteById(String code);

    @Query(nativeQuery = true, value = "select * from public.products where client_id =?1 order by id asc ;")
    List<Products> findAllByClientId(String clientId);


    @Query(nativeQuery = true, value = "select * from public.products where client_id =?1 order by id asc;")
    List<Products> getAllByClientId(String clientId);
}
