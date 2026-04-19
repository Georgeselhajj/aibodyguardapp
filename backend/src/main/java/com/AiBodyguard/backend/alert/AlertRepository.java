package com.AiBodyguard.backend.alert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByUserEmailOrderByCreatedAtDesc(String userEmail);
}
