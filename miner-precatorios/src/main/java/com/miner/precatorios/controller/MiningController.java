package com.miner.precatorios.controller;

import com.miner.precatorios.dto.FilterRequest;
import com.miner.precatorios.model.User;
import com.miner.precatorios.repository.UserRepository;
import com.miner.precatorios.service.MiningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/precatorios")
public class MiningController {

    @Autowired
    private MiningService service;
    
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody FilterRequest filters, 
                                    @RequestParam int limit) {
        try {
            // Pega o email de quem está logado automaticamente pelo Token
            String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado no banco"));

            return ResponseEntity.ok(service.realizarMineracao(filters, limit, user.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(403).body("Erro: " + e.getMessage());
        }
    }
}