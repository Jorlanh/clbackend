package com.miner.precatorios.service;

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
    private DataJudClient dataJudClient; // Cliente para conectar com sua chave API

    // --- CAMADA 3: A INTELIGÊNCIA (LOA) ---
    // Regra: Apresentado até 02/04 -> LOA N+1. Depois -> LOA N+2.
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

    @Transactional(rollbackFor = Exception.class) // Garante consistência financeira (Rollback em caso de erro)
    public List<Precatorio> realizarMineracao(FilterRequest filters, int limit, Long userId) {
        // 1. Verifica Saldo Antes de tudo
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        if (user.getCredits() < limit) {
            throw new RuntimeException("Saldo insuficiente. Seus créditos: " + user.getCredits());
        }

        // Tenta conectar no DataJud real (apenas para validar a chave no log se necessário)
        // dataJudClient.buscarProcessosReais(); 

        List<Precatorio> resultados = new ArrayList<>();

        // --- CAMADA 1: PROCESSOS (Simulação Inteligente) ---
        // Loop de geração/busca dos dados
        for (int i = 0; i < limit; i++) {
            Precatorio p = new Precatorio();
            
            // Dados básicos (Simulando resposta do Tribunal)
            p.setTribunal(filters.getTribunal() != null && !filters.getTribunal().isEmpty() ? filters.getTribunal() : "TJSP");
            p.setUf("SP");
            p.setRegiao("Estadual");
            p.setNumeroProcesso("00" + ThreadLocalRandom.current().nextInt(10000, 99999) + "-88.2024.8.26.0000");
            p.setNumeroPrecatorio("PRC" + ThreadLocalRandom.current().nextInt(1000, 9999) + "/2024");
            p.setValor(BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(30000, 500000)));
            p.setNatureza(i % 2 == 0 ? "Alimentar" : "Comum");
            p.setSituacao("Aguardando Pagamento");
            
            // Aplica a Lógica da LOA na data gerada
            LocalDate dataGerada = LocalDate.now().minusMonths(ThreadLocalRandom.current().nextInt(1, 24));
            p.setDataExpedicao(dataGerada);
            p.setAno(dataGerada.getYear());
            p.setLoa(calcularLOA(dataGerada)); // <--- Lógica aplicada aqui
            
            // Dados do Titular
            p.setNomeTitular("Credor Exemplo " + ThreadLocalRandom.current().nextInt(1000, 9999));
            p.setCpf("***.123.456-**");

            // --- CAMADA 2: CONTATO (Enriquecimento) ---
            // Aqui a chamada para BigDataCorp está simulada para não gerar custos agora.
            enrichContactData(p);
            
            // Aplica Filtros do Frontend
            boolean passaFiltro = filters.getLoa() == null || filters.getLoa().isEmpty() || filters.getLoa().equals("Todas as LOAs") || p.getLoa().equals(filters.getLoa());

            if (passaFiltro) {
                resultados.add(p);
            }
        }

        // 2. O Desconto Real Acontece Aqui
        // Debita Créditos e Atualiza Estatísticas
        user.setCredits(user.getCredits() - resultados.size());
        user.setTotalSearches(user.getTotalSearches() + 1);
        user.setTotalRecordsExtracted(user.getTotalRecordsExtracted() + resultados.size());
        
        userRepository.save(user); // Persiste a transação no banco

        return resultados;
    }

    private void enrichContactData(Precatorio p) {
        // Simula o retorno de uma API PAGA (BigDataCorp / Assertiva)
        p.setWhatsapp("+55 11 9" + ThreadLocalRandom.current().nextInt(1000, 9999) + "-" + ThreadLocalRandom.current().nextInt(1000, 9999));
        p.setEmail("contato." + p.getNomeTitular().toLowerCase().replace(" ", ".") + "@gmail.com");
    }
}