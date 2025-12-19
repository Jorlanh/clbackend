package com.miner.precatorios.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DataJudClient {

    // INJEÇÃO SEGURA: O valor vem do application.properties ou Variável de Ambiente
    @Value("${api.datajud.key}")
    private String apiKey;

    private final String URL = "https://api-publica.datajud.cnj.jus.br/api_publica_tjsp/_search";

    public void buscarProcessosReais() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        
        // Usa a variável injetada, não o texto hardcoded
        headers.set("Authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = "{ \"query\": { \"match_all\": {} }, \"size\": 5 }";

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(URL, HttpMethod.POST, entity, String.class);
            System.out.println("Conexão com DataJud realizada com sucesso. Status: " + response.getStatusCode());
        } catch (Exception e) {
            System.err.println("Erro ao conectar no DataJud (Verifique a Chave no application.properties): " + e.getMessage());
        }
    }
}