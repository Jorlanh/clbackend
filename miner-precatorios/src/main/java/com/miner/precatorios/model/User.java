package com.miner.precatorios.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "users") // "user" é palavra reservada em alguns bancos
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;
    private String password; 
    private String name;
    private String role; // "user" ou "master"
    
    private int credits; // Saldo de Créditos

    // Estatísticas para o Dashboard
    private int totalSearches;
    private int totalRecordsExtracted;
    private LocalDate joinDate;
}