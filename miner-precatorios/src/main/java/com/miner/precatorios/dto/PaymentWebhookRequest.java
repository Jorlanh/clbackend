package com.miner.precatorios.dto;
import lombok.Data;
@Data public class PaymentWebhookRequest { private String emailUser; private int creditsToAdd; }