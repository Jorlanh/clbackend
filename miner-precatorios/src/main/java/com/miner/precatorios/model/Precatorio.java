package com.miner.precatorios.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
public class Precatorio {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Dados do Tribunal (DataJud)
    private String numeroProcesso;
    private String numeroPrecatorio;
    private String tribunal;
    private String regiao;
    private String uf;
    private Integer ano; 
    
    private String natureza;
    private BigDecimal valor;
    
    private String nomeTitular;
    private String cpf;
    
    // Dados Enriquecidos (Camada de Contato)
    private String whatsapp;
    private String email;
    
    private String situacao;
    private String orgao;
    
    // Lógica da Inteligência (LOA)
    private LocalDate dataExpedicao;
    private String loa; // LOA 2025, LOA 2026...
}