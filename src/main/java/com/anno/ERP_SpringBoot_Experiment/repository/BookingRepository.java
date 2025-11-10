package com.anno.ERP_SpringBoot_Experiment.repository;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
}
