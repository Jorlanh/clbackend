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
            // Ajusta URL se for TRF ou outro tribunal
            String url = URL_BASE; 
            if (filters.getTribunal() != null && !filters.getTribunal().isEmpty()) {
                if (filters.getTribunal().toLowerCase().contains("trf")) {
                    url = "https://api-publica.datajud.cnj.jus.br/api_publica_trf1/_search";
                } 
                // Adicione outros mapeamentos de URL conforme necessário
            }

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return response.getBody();

        } catch (Exception e) {
            System.err.println("Erro na requisição DataJud: " + e.getMessage());
            return null;
        }
    }

    private String montarQueryElasticsearch(FilterRequest filters, int limit) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"query\": { \"bool\": { \"must\": [");

        // 1. Filtro de ANO (Range de Datas)
        if (filters.getAno() != null && !filters.getAno().isEmpty()) {
            String ano = filters.getAno();
            sb.append(String.format(
                "{ \"range\": { \"dataAjuizamento\": { \"gte\": \"%s-01-01\", \"lte\": \"%s-12-31\" } } },", 
                ano, ano
            ));
        }

        // 2. Filtro de NATUREZA (Busca nos Assuntos)
        if (filters.getNatureza() != null && !filters.getNatureza().isEmpty()) {
            if (filters.getNatureza().equalsIgnoreCase("Alimentar")) {
                // Busca palavras-chave de natureza alimentar
                sb.append("{ \"query_string\": { \"default_field\": \"assuntos.nome\", \"query\": \"*alimentos* OR *salário* OR *previdenciário* OR *indenização*\" } },");
            } else if (filters.getNatureza().equalsIgnoreCase("Comum")) {
                // Tenta excluir termos alimentares (busca simples)
                sb.append("{ \"query_string\": { \"default_field\": \"assuntos.nome\", \"query\": \"*tributário* OR *execução* OR *cobrança*\" } },");
            }
        }

        // 3. Filtro de SITUAÇÃO (Busca nos Movimentos)
        if (filters.getSituacao() != null && !filters.getSituacao().isEmpty()) {
            if (filters.getSituacao().equalsIgnoreCase("Pago")) {
                sb.append("{ \"query_string\": { \"default_field\": \"movimentos.nome\", \"query\": \"*pagamento* OR *expedição* OR *levantamento*\" } },");
            } else if (filters.getSituacao().equalsIgnoreCase("Cancelado")) {
                sb.append("{ \"query_string\": { \"default_field\": \"movimentos.nome\", \"query\": \"*cancelado* OR *arquivado*\" } },");
            }
            // "Aguardando Pagamento" é o default, então não filtramos para trazer o resto
        }

        // Remove a última vírgula se houver
        if (sb.toString().endsWith(",")) {
            sb.setLength(sb.length() - 1);
        } else {
            // Se não tiver nenhum filtro específico, garante que traga algo
            if (!sb.toString().contains("{")) { 
                 sb.append("{ \"match_all\": {} }");
            } else {
                 // Caso tenha entrado nos ifs mas sem vírgula (raro), match_all garante sintaxe
                 // Melhor abordagem: se a string terminar em '[', adiciona match_all
                 if (sb.toString().endsWith("[")) sb.append("{ \"match_all\": {} }");
            }
        }

        sb.append("] } }, \"size\": " + limit + " }");
        
        return sb.toString();
    }
}