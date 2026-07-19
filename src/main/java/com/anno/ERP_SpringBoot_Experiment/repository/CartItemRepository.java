package com.anno.ERP_SpringBoot_Experiment.repository;

import com.anno.ERP_SpringBoot_Experiment.model.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
