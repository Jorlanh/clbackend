package com.miner.precatorios.dto;
import lombok.Data;
@Data
public class FilterRequest {
    private String tribunal;
    private String ano;
    private String uf;
    private String faixaValorMin;
    private String faixaValorMax;
    private String situacao;
    private String nomeTitular;
    private String cpf;
    private String natureza;
    private String numeroProcesso;
    private String numeroPrecatorio;
    private String loa;
}