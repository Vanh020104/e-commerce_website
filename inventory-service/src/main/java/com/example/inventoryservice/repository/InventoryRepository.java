package com.example.inventoryservice.repository;

import com.example.inventoryservice.entities.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Page<Inventory> findInventoryByProductIdAndDeletedAtIsNull(Long productId, Pageable pageable);
    Page<Inventory> findByDeletedAtIsNull(Pageable pageable);
    Page<Inventory> findByDeletedAtIsNotNull(Pageable pageable);
    Page<Inventory> findInventoryByInventoryStatusIdAndDeletedAtIsNull(Integer inventoryStatusId, Pageable pageable);

}
