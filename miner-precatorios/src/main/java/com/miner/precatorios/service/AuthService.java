package com.miner.precatorios.service;

import com.miner.precatorios.dto.RegisterRequest;
import com.miner.precatorios.model.User;
import com.miner.precatorios.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    public User register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email já cadastrado.");
        }
        User newUser = new User();
        newUser.setName(request.getName());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(request.getPassword());
        newUser.setRole("user");
        newUser.setCredits(0); // Começa zerado
        newUser.setJoinDate(LocalDate.now());
        
        return userRepository.save(newUser);
    }

    public User login(String email, String password) {
        // Backdoor do Master (Solicitado anteriormente)
        if ("sachabm@gmail.com".equals(email) && "Sb7548$".equals(password)) {
            User master = new User();
            master.setId(999L); // ID fictício
            master.setName("Sacha Master");
            master.setEmail(email);
            master.setRole("master");
            master.setCredits(99999);
            return master;
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Senha incorreta.");
        }
        return user;
    }
}