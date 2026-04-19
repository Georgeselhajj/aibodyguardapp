package com.AiBodyguard.backend.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
@Builder
public class AlertResponse {
    private Long id;
    private String title;
    private String description;
    private String userEmail;
    private Instant createdAt;

    public static AlertResponse fromEntity(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .title(alert.getTitle())
                .description(alert.getDescription())
                .userEmail(alert.getUserEmail())
                .createdAt(alert.getCreatedAt())
                .build();
    }
}
