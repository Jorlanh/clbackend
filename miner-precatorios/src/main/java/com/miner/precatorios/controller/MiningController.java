package com.miner.precatorios.controller;

import com.miner.precatorios.dto.FilterRequest;
import com.miner.precatorios.service.MiningService;
import com.miner.precatorios.repository.UserRepository;
import com.miner.precatorios.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
                                    @RequestParam int limit,
                                    @RequestHeader(value = "User-Email", required = false) String userEmail) {
        try {
            // Em produção real, pegamos o ID do token JWT.
            // Para seu MVP funcionar fácil, vamos pegar o ID pelo email passado no Header ou usar um padrão.
            
            Long userId = 1L; // Padrão
            
            if(userEmail != null) {
                User user = userRepository.findByEmail(userEmail).orElse(null);
                if(user != null) userId = user.getId();
            }

            return ResponseEntity.ok(service.realizarMineracao(filters, limit, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}