package com.miner.precatorios.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miner.precatorios.client.DataJudClient;
import com.miner.precatorios.dto.FilterRequest;
import com.miner.precatorios.model.Precatorio;
import com.miner.precatorios.model.User;
import com.miner.precatorios.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MiningService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DataJudClient dataJudClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 1. CÁLCULO DA LOA (Regra Constitucional)
    private String calcularLOA(LocalDate dataExpedicao) {
        if (dataExpedicao == null) return "Indefinido";
        int ano = dataExpedicao.getYear();
        LocalDate dataCorte = LocalDate.of(ano, 4, 2);
        
        if (dataExpedicao.isBefore(dataCorte) || dataExpedicao.equals(dataCorte)) {
            return "LOA " + (ano + 1);
        } else {
            return "LOA " + (ano + 2);
        }
    }

    // 2. EXTRAÇÃO REAL DE NATUREZA
    private String extrairNaturezaReal(JsonNode source) {
        if (source.has("assuntos")) {
            for (JsonNode assunto : source.get("assuntos")) {
                String nome = assunto.path("nome").asText().toLowerCase();
                if (nome.contains("alimentos") || 
                    nome.contains("salário") || 
                    nome.contains("vencimento") || 
                    nome.contains("previdenciário") || 
                    nome.contains("benefício") ||
                    nome.contains("indeniza")) {
                    return "Alimentar";
                }
            }
        }
        return "Comum";
    }

    // 3. EXTRAÇÃO REAL DE SITUAÇÃO
    private String extrairSituacaoReal(JsonNode source) {
        // Padrão inicial
        String status = "Aguardando Pagamento";
        
        if (source.has("movimentos")) {
            for (JsonNode mov : source.get("movimentos")) {
                String nomeMov = mov.path("nome").asText().toLowerCase();
                
                // Prioridade: Pagamento > Cancelamento > Processamento
                if (nomeMov.contains("pagamento") || nomeMov.contains("levantamento") || nomeMov.contains("quitado") || nomeMov.contains("expedição")) {
                    return "Pago";
                }
                if (nomeMov.contains("cancelado") || nomeMov.contains("arquivado") || nomeMov.contains("extinto")) {
                    return "Cancelado";
                }
                if (nomeMov.contains("processamento") || nomeMov.contains("cálculo")) {
                    status = "Em Processamento";
                }
            }
        }
        return status;
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Precatorio> realizarMineracao(FilterRequest filters, int limit, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (user.getCredits() < limit) {
            throw new RuntimeException("Saldo insuficiente.");
        }

        List<Precatorio> resultados = new ArrayList<>();

        try {
            // Busca 4x o limite para ter sobra caso os filtros descartem muitos itens
            String jsonResposta = dataJudClient.buscarProcessosReais(filters, limit * 4);
            
            if (jsonResposta != null) {
                JsonNode root = objectMapper.readTree(jsonResposta);
                JsonNode hits = root.path("hits").path("hits");

                if (hits.isArray()) {
                    for (JsonNode hit : hits) {
                        if (resultados.size() >= limit) break;

                        JsonNode source = hit.path("_source");
                        Precatorio p = new Precatorio();

                        // --- DADOS REAIS ---
                        p.setNumeroProcesso(source.path("numeroProcesso").asText("N/A"));
                        p.setTribunal(source.path("tribunal").asText(filters.getTribunal() != null ? filters.getTribunal() : "TJSP"));
                        p.setUf("SP"); 

                        // Extração Inteligente
                        p.setNatureza(extrairNaturezaReal(source));
                        p.setSituacao(extrairSituacaoReal(source));

                        // Data e LOA
                        String dataAjuizamento = source.path("dataAjuizamento").asText();
                        if (dataAjuizamento != null && dataAjuizamento.length() >= 10) {
                            try {
                                LocalDate dataReal = LocalDate.parse(dataAjuizamento.substring(0, 10));
                                p.setDataExpedicao(dataReal);
                                p.setAno(dataReal.getYear());
                                p.setLoa(calcularLOA(dataReal));
                            } catch (Exception e) {
                                p.setAno(2024);
                                p.setLoa("Indefinido");
                            }
                        } else {
                            // Se não tem data e estamos filtrando LOA, pula
                            if (filters.getLoa() != null && !filters.getLoa().isEmpty()) continue;
                            p.setAno(2024);
                            p.setLoa("Indefinido");
                        }

                        // --- FILTRAGEM DE SEGURANÇA (Garante que o retorno bate com o filtro) ---
                        
                        // 1. Filtro de LOA
                        if (filters.getLoa() != null && !filters.getLoa().isEmpty() && !filters.getLoa().equals("Todas as LOAs")) {
                            if (!filters.getLoa().equals(p.getLoa())) continue;
                        }

                        // 2. Filtro de Natureza
                        if (filters.getNatureza() != null && !filters.getNatureza().isEmpty()) {
                            if (!filters.getNatureza().equalsIgnoreCase(p.getNatureza())) continue;
                        }

                        // 3. Filtro de Situação
                        if (filters.getSituacao() != null && !filters.getSituacao().isEmpty()) {
                            if (!filters.getSituacao().equalsIgnoreCase(p.getSituacao())) continue;
                        }

                        // --- VALORES E CPF (Simulados por LGPD) ---
                        
                        // CORREÇÃO DOS VALORES: Padrão 0.0 e MAX_VALUE (Infinito)
                        double minVal = (filters.getFaixaValorMin() != null && !filters.getFaixaValorMin().isEmpty()) 
                                        ? Double.parseDouble(filters.getFaixaValorMin()) : 0.0;
                        
                        double maxVal = (filters.getFaixaValorMax() != null && !filters.getFaixaValorMax().isEmpty()) 
                                        ? Double.parseDouble(filters.getFaixaValorMax()) : Double.MAX_VALUE;
                        
                        // Ajuste de segurança caso o usuário inverta (ex: min 100, max 50)
                        if (minVal > maxVal && maxVal != Double.MAX_VALUE) { 
                            double temp = minVal; minVal = maxVal; maxVal = temp; 
                        }
                        
                        // Limita o teto aleatório se não houver filtro (para não gerar trilhões)
                        if (maxVal == Double.MAX_VALUE) maxVal = 500000.0;

                        p.setValor(BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(minVal, maxVal)));

                        // Preenchimento Visual
                        p.setNumeroPrecatorio("PRC" + ThreadLocalRandom.current().nextInt(10000, 99999) + "/" + p.getAno());
                        p.setNomeTitular("Beneficiário " + p.getNumeroProcesso().substring(0, 5));
                        p.setCpf("***.123.456-**"); 
                        
                        enrichContactData(p); 

                        resultados.add(p);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erro ao processar dados: " + e.getMessage());
        }

        if (resultados.isEmpty()) {
            throw new RuntimeException("Nenhum processo encontrado com esses filtros. Tente filtros mais amplos.");
        }

        user.setCredits(user.getCredits() - resultados.size());
        user.setTotalSearches(user.getTotalSearches() + 1);
        user.setTotalRecordsExtracted(user.getTotalRecordsExtracted() + resultados.size());
        userRepository.save(user);

        return resultados;
    }

    private void enrichContactData(Precatorio p) {
        // APENAS CONTATO. NÃO MEXE MAIS NA SITUAÇÃO AQUI!
        p.setWhatsapp("+55 11 9" + ThreadLocalRandom.current().nextInt(1000, 9999) + "-" + ThreadLocalRandom.current().nextInt(1000, 9999));
        p.setEmail("contato.pendente@email.com");
        
        // CORREÇÃO: Removemos a linha abaixo que causava o bug
        // p.setSituacao("Aguardando Pagamento"); <--- ISSO ESTAVA SOBRESCREVENDO TUDO!
    }
}