package com.qcg.repository;

import com.qcg.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByUserIdOrderByAppointmentTimeDesc(Long userId);
    List<Appointment> findByShopIdOrderByAppointmentTimeDesc(Long shopId);
}
