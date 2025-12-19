package com.miner.precatorios.client;

import com.miner.precatorios.dto.FilterRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DataJudClient {

    @Value("${api.datajud.key}")
    private String apiKey;

    private final String URL_BASE = "https://api-publica.datajud.cnj.jus.br/api_publica_tjsp/_search";

    public String buscarProcessosReais(FilterRequest filters, int limit) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        
        headers.set("Authorization", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Constrói a Query do Elasticsearch dinamicamente
        String queryJson = montarQueryElasticsearch(filters, limit);

        HttpEntity<String> entity = new HttpEntity<>(queryJson, headers);

        try {
            // Ajusta URL se for TRF (Exemplo simples)
            String url = URL_BASE; 
            if (filters.getTribunal() != null && filters.getTribunal().toLowerCase().contains("trf")) {
                url = "https://api-publica.datajud.cnj.jus.br/api_publica_trf1/_search";
            }

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return response.getBody();

        } catch (Exception e) {
            System.err.println("Erro na requisição DataJud: " + e.getMessage());
            return null;
        }
    }

    private String montarQueryElasticsearch(FilterRequest filters, int limit) {
        // Início da query bool
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"query\": { \"bool\": { \"must\": [");

        // 1. Filtro de Tribunal (se houver, embora a URL já segmente)
        // sb.append("{ \"match\": { \"tribunal\": \"" + filters.getTribunal() + "\" } }");

        // 2. Filtro de ANO (A Mágica acontece aqui)
        // O campo dataAjuizamento no DataJud é ISO (ex: 2023-05-12T10:00:00)
        if (filters.getAno() != null && !filters.getAno().isEmpty()) {
            String ano = filters.getAno();
            // Adiciona vírgula se já tiver itens no array (aqui é o primeiro, então não precisa)
            sb.append(String.format(
                "{ \"range\": { \"dataAjuizamento\": { \"gte\": \"%s-01-01\", \"lte\": \"%s-12-31\" } } }", 
                ano, ano
            ));
        } else {
            // Se não tem filtro, busca tudo (match_all é default quando must está vazio, mas vamos garantir algo)
            sb.append("{ \"match_all\": {} }");
        }

        sb.append("] } }, \"size\": " + limit + " }");
        
        return sb.toString();
    }
}