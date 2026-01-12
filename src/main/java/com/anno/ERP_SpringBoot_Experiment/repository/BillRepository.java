package com.anno.ERP_SpringBoot_Experiment.repository;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BillRepository extends JpaRepository<Bill, UUID> {

    /**
     * Kiểm tra xem Bill đã tồn tại cho Order này chưa (Idempotency)
     */
    boolean existsByOrder_Id(UUID orderId);

    /**
     * Tìm Bill theo Order ID
     */
    Optional<Bill> findByOrder_Id(UUID orderId);
}
