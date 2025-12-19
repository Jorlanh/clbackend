package com.miner.precatorios.controller;

import com.miner.precatorios.model.User;
import com.miner.precatorios.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private UserRepository userRepository;

    // Defina isso no application.properties: payment.webhook.secret=minhaSenhaSuperSecreta123
    @Value("${payment.webhook.secret:padrao}") 
    private String webhookSecret;

    @PostMapping("/webhook")
    public ResponseEntity<?> handleRealPayment(
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret,
            @RequestBody Map<String, Object> payload) {

        // 1. Segurança: Verifica se quem chamou foi realmente o Gateway de Pagamento
        // (Configure seu gateway para enviar esse header)
        if (secret == null || !secret.equals(webhookSecret)) {
            return ResponseEntity.status(403).body("Acesso negado: Segredo inválido.");
        }

        try {
            // 2. Extração de Dados (Exemplo genérico - Adapte para seu Gateway: Stripe/Asaas)
            // Geralmente gateways enviam "customer_email" e "amount"
            String email = (String) payload.get("email"); 
            Number valorPago = (Number) payload.get("amount"); // Valor em Reais

            if (email == null || valorPago == null) {
                return ResponseEntity.badRequest().body("Payload inválido.");
            }

            // 3. Busca Usuário Real
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + email));

            // 4. Conversão Real: R$ 1,00 = 10 Créditos (Exemplo)
            int creditosComprados = valorPago.intValue() * 10;
            
            user.setCredits(user.getCredits() + creditosComprados);
            userRepository.save(user);

            return ResponseEntity.ok("Sucesso! Adicionado " + creditosComprados + " créditos para " + email);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro ao processar pagamento.");
        }
    }
}