package com.miner.precatorios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor // Cria o construtor com todos os argumentos automaticamente
public class LoginResponse {
    private String token;
    private Long id;
    private String name;
    private String email;
    private String role;
    private int credits;
    private int totalSearches;
    private int totalRecordsExtracted;
}