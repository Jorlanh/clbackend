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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MiningService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DataJudClient dataJudClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // LÓGICA DE NEGÓCIO: REGRA DA LOA (Constituição Federal)
    private String calcularLOA(LocalDate dataExpedicao) {
        if (dataExpedicao == null) return "Indefinido";
        int ano = dataExpedicao.getYear();
        LocalDate dataCorte = LocalDate.of(ano, 4, 2); // 02 de Abril
        
        if (dataExpedicao.isBefore(dataCorte) || dataExpedicao.equals(dataCorte)) {
            return "LOA " + (ano + 1);
        } else {
            return "LOA " + (ano + 2);
        }
    }

    // --- NOVA LÓGICA: NATUREZA REAL ---
    private String extrairNaturezaReal(JsonNode source) {
        // Varre os "assuntos" do processo em busca de palavras-chave
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
        return "Comum"; // Padrão se não achar palavras de cunho alimentar
    }

    // --- NOVA LÓGICA: SITUAÇÃO REAL ---
    private String extrairSituacaoReal(JsonNode source) {
        // A situação depende do último movimento relevante
        String status = "Aguardando Pagamento"; // Padrão inicial
        
        if (source.has("movimentos")) {
            for (JsonNode mov : source.get("movimentos")) {
                String nomeMov = mov.path("nome").asText().toLowerCase();
                
                if (nomeMov.contains("pagamento") || nomeMov.contains("levantamento") || nomeMov.contains("quitado")) {
                    status = "Pago";
                    break; // Se achou pagamento, para e define como Pago
                }
                if (nomeMov.contains("cancelado") || nomeMov.contains("arquivado") || nomeMov.contains("baixa definitiva")) {
                    status = "Cancelado";
                }
                if (nomeMov.contains("em processamento") || nomeMov.contains("cálculo")) {
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
            // Busca mais registros para poder filtrar depois
            String jsonResposta = dataJudClient.buscarProcessosReais(filters, limit * 4);
            
            if (jsonResposta != null) {
                JsonNode root = objectMapper.readTree(jsonResposta);
                JsonNode hits = root.path("hits").path("hits");

                if (hits.isArray()) {
                    for (JsonNode hit : hits) {
                        if (resultados.size() >= limit) break;

                        JsonNode source = hit.path("_source");
                        Precatorio p = new Precatorio();

                        // 1. DADOS BÁSICOS
                        p.setNumeroProcesso(source.path("numeroProcesso").asText("N/A"));
                        p.setTribunal(source.path("tribunal").asText(filters.getTribunal() != null ? filters.getTribunal() : "TJSP"));
                        p.setUf("SP"); 

                        // 2. EXTRAÇÃO REAL DE NATUREZA E SITUAÇÃO
                        p.setNatureza(extrairNaturezaReal(source));
                        p.setSituacao(extrairSituacaoReal(source));

                        // 3. EXTRAÇÃO DE DATA E CÁLCULO LOA
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
                            if (filters.getLoa() != null && !filters.getLoa().isEmpty()) continue;
                            p.setAno(2024);
                            p.setLoa("Indefinido");
                        }

                        // --- FILTRAGEM RIGOROSA (Backend Validation) ---
                        
                        // Filtro de LOA
                        if (filters.getLoa() != null && !filters.getLoa().isEmpty() && !filters.getLoa().equals("Todas as LOAs")) {
                            if (!filters.getLoa().equals(p.getLoa())) continue;
                        }

                        // Filtro de Natureza (Se o usuário pediu Comum, e o processo é Alimentar, pula)
                        if (filters.getNatureza() != null && !filters.getNatureza().isEmpty()) {
                            if (!filters.getNatureza().equalsIgnoreCase(p.getNatureza())) continue;
                        }

                        // Filtro de Situação
                        if (filters.getSituacao() != null && !filters.getSituacao().isEmpty()) {
                            if (!filters.getSituacao().equalsIgnoreCase(p.getSituacao())) continue;
                        }

                        // --- SIMULAÇÃO DE DADOS SENSÍVEIS (Valor e CPF) ---
                        // (Necessário pois DataJud não entrega valor atualizado nem CPF aberto)
                        
                        double minVal = (filters.getFaixaValorMin() != null && !filters.getFaixaValorMin().isEmpty()) 
                                        ? Double.parseDouble(filters.getFaixaValorMin()) : 30000.0;
                        double maxVal = (filters.getFaixaValorMax() != null && !filters.getFaixaValorMax().isEmpty()) 
                                        ? Double.parseDouble(filters.getFaixaValorMax()) : 500000.0;
                        
                        if (minVal > maxVal) { double temp = minVal; minVal = maxVal; maxVal = temp; }
                        
                        // Gera valor DENTRO da faixa pedida
                        p.setValor(BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(minVal, maxVal)));

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
            throw new RuntimeException("Erro ao processar dados reais: " + e.getMessage());
        }

        if (resultados.isEmpty()) {
            throw new RuntimeException("Nenhum processo encontrado com esses critérios exatos.");
        }

        user.setCredits(user.getCredits() - resultados.size());
        user.setTotalSearches(user.getTotalSearches() + 1);
        user.setTotalRecordsExtracted(user.getTotalRecordsExtracted() + resultados.size());
        userRepository.save(user);

        return resultados;
    }

    private void enrichContactData(Precatorio p) {
        // Dados de contato continuam simulados até contratar BigDataCorp
        p.setWhatsapp("+55 11 9" + ThreadLocalRandom.current().nextInt(1000, 9999) + "-" + ThreadLocalRandom.current().nextInt(1000, 9999));
        p.setEmail("contato.pendente@email.com");
    }
}