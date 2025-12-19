package com.miner.precatorios.service;

import com.miner.precatorios.dto.LoginResponse; // Certifique-se de ter criado este DTO
import com.miner.precatorios.dto.RegisterRequest;
import com.miner.precatorios.model.User;
import com.miner.precatorios.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Injeta criptografia

    @Autowired
    private TokenService tokenService; // Injeta gerador de JWT

    public User register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email já cadastrado.");
        }
        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setEmail(request.getEmail());
        
        // INCREMENTO: Criptografa a senha para segurança real
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        
        newUser.setRole("user");
        newUser.setCredits(0); // Começa zerado
        newUser.setJoinDate(LocalDate.now());
        
        return userRepository.save(newUser);
    }

    public LoginResponse login(String email, String password) {
        // --- BACKDOOR DO MASTER (Mantido) ---
        if ("sachabm@gmail.com".equals(email) && "Sb7548$".equals(password)) {
            User master = new User();
            master.setId(999L);
            master.setName("Sacha Master");
            master.setEmail(email);
            master.setRole("master");
            master.setCredits(99999);
            
            // Incremento: Gera token real para o Master acessar a API
            String token = tokenService.generateToken(master);
            
            return new LoginResponse(token, master.getId(), master.getName(), master.getEmail(), master.getRole(), master.getCredits(), 0, 0);
        }

        // --- LOGIN PADRÃO REAL ---
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        
        // Incremento: Verifica a senha criptografada
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Senha incorreta.");
        }

        // Incremento: Gera o Token JWT
        String token = tokenService.generateToken(user);

        return new LoginResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCredits(), user.getTotalSearches(), user.getTotalRecordsExtracted());
    }
}