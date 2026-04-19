package com.AiBodyguard.backend.alert;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertResponse createAlert(CreateAlertRequest request) {
        Alert alert = Alert.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .userEmail(request.getUserEmail())
                .createdAt(Instant.now())
                .build();

        return AlertResponse.fromEntity(alertRepository.save(alert));
    }

    public List<AlertResponse> listAlerts(String userEmail) {
        return alertRepository.findByUserEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(AlertResponse::fromEntity)
                .toList();
    }
}
