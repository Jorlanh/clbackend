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
            // 1. ADMIN PADRÃO (Para testes ou emergência)
            if (repository.findByEmail("admin@miner.com").isEmpty()) {
                User admin = new User();
                admin.setName("Administrador Master");
                admin.setEmail("admin@miner.com"); // Login
                admin.setPassword(passwordEncoder.encode("admin123")); // Senha
                admin.setRole("ADMIN");
                admin.setCredits(10000);
                admin.setJoinDate(LocalDate.now());
                admin.setTotalSearches(0);
                admin.setTotalRecordsExtracted(0);
                
                repository.save(admin);
                System.out.println("✅ ADMIN PADRÃO CRIADO: admin@miner.com");
            }

            // 2. SEU LOGIN PESSOAL (Sacha)
            if (repository.findByEmail("sachabm@gmail.com").isEmpty()) {
                User sacha = new User();
                sacha.setName("Sacha Master");
                sacha.setEmail("sachabm@gmail.com"); // Seu email real
                
                // Sua senha segura (codificada)
                sacha.setPassword(passwordEncoder.encode("Sb7548$")); 
                
                sacha.setRole("MASTER"); // Define como Master/Admin
                sacha.setCredits(99999); // Créditos ilimitados
                sacha.setJoinDate(LocalDate.now());
                sacha.setTotalSearches(0);
                sacha.setTotalRecordsExtracted(0);

                repository.save(sacha);
                System.out.println("✅ LOGIN SACHA CRIADO: sachabm@gmail.com");
            }
        };
    }
}