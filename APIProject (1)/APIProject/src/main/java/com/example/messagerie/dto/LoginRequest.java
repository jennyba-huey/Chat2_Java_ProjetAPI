package com.example.messagerie.dto;

import jakarta.validation.constraints.NotBlank;

/** Corps JSON de POST /api/auth/login */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String motDePasse
) {}
