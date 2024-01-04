package uz.dataFin.notificationbot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.dataFin.notificationbot.model.WarehouseDTO;

import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<WarehouseDTO, Long> {
    Optional<WarehouseDTO> findByUniqueID(String uniqueID);

    @Query(nativeQuery = true, value = "select * from warehousedto where name = ?1 and is_branch = false")
    Optional<WarehouseDTO> findByNameAAndIsBranch(String name, Boolean isBranch);

    @Query(nativeQuery = true, value = "select * from warehousedto where name = ?1 and is_branch = true")
    Optional<WarehouseDTO> findByNameAAndNotIsBranch(String name, Boolean isBranch);

}
