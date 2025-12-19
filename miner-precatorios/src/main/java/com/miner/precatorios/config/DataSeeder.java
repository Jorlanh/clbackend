package com.miner.precatorios.config;

import com.miner.precatorios.model.User;
import com.miner.precatorios.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(UserRepository repository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Verifica se o admin já existe para não criar duplicado
            if (repository.findByEmail("admin@miner.com").isEmpty()) {
                User admin = new User();
                admin.setName("Administrador Master");
                admin.setEmail("admin@miner.com");
                // Senha criptografada real
                admin.setPassword(passwordEncoder.encode("admin123")); 
                admin.setRole("ADMIN");
                admin.setCredits(10000); // Começa com 10k
                admin.setJoinDate(LocalDate.now());
                admin.setTotalSearches(0);
                admin.setTotalRecordsExtracted(0);
                
                repository.save(admin);
                System.out.println("✅ USUÁRIO ADMIN CRIADO COM 10.000 CRÉDITOS!");
            }
        };
    }
}