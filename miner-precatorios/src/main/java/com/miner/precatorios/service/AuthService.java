package com.miner.precatorios.service;

import com.miner.precatorios.dto.LoginResponse;
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
    private PasswordEncoder passwordEncoder; // Injeta a Criptografia

    @Autowired
    private TokenService tokenService; // Injeta o Gerador de JWT

    public User register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email já cadastrado.");
        }
        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setEmail(request.getEmail());
        
        // --- SEGURANÇA: Criptografa a senha antes de salvar ---
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        
        newUser.setRole("user");
        newUser.setCredits(0); // Começa zerado
        newUser.setJoinDate(LocalDate.now());
        
        return userRepository.save(newUser);
    }

    public LoginResponse login(String email, String password) {
        // --- BACKDOOR DO MASTER (Mantido conforme solicitado) ---
        // Nota: Como o Master não está no banco com senha hash, validamos manualmente
        if ("sachabm@gmail.com".equals(email) && "Sb7548$".equals(password)) {
            User master = new User();
            master.setId(999L); 
            master.setName("Sacha Master");
            master.setEmail(email);
            master.setRole("master");
            master.setCredits(99999);
            
            // Gera token para o Master também
            String token = tokenService.generateToken(master);
            
            return new LoginResponse(
                token, 
                master.getId(), 
                master.getName(), 
                master.getEmail(), 
                master.getRole(), 
                master.getCredits(), 
                0, 0
            );
        }

        // --- LOGIN PADRÃO (SEGURO) ---
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        
        // Verifica a senha criptografada (BCrypt)
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Senha incorreta.");
        }

        // --- GERA O TOKEN JWT ---
        String token = tokenService.generateToken(user);

        // Retorna o objeto com o Token para o Frontend salvar no LocalStorage
        return new LoginResponse(
            token,
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.getCredits(),
            user.getTotalSearches(),
            user.getTotalRecordsExtracted()
        );
    }
}