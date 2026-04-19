package com.AiBodyguard.backend.alert;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAlertRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @Email(message = "Enter a valid email address")
    @NotBlank(message = "User email is required")
    private String userEmail;
}
