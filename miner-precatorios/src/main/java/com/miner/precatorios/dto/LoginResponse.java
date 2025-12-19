package com.miner.precatorios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token; // O Frontend precisa disso para o Header
    private Long id;
    private String name;
    private String email;
    private String role;
    private int credits;
    
    // Stats simples
    private int totalSearches;
    private int totalRecordsExtracted;
}