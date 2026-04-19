package com.AiBodyguard.backend.alert;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @PostMapping
    public AlertResponse create(@Valid @RequestBody CreateAlertRequest request) {
        return alertService.createAlert(request);
    }

    @GetMapping
    public List<AlertResponse> list(@RequestParam("userEmail") @NotBlank String userEmail) {
        return alertService.listAlerts(userEmail);
    }
}
