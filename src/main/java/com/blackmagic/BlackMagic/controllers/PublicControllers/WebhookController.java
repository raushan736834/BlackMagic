package com.blackmagic.BlackMagic.controllers.PublicControllers;

import com.blackmagic.BlackMagic.dtos.publicDtos.RazorpayWebhookPayload;
import com.blackmagic.BlackMagic.services.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class WebhookController {

    private final PaymentService paymentService;

    @PostMapping("/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestHeader("X-Razorpay-Signature") String signature,
            @RequestBody RazorpayWebhookPayload payload) {
        log.info("Received Razorpay webhook: {}", payload.getEvent());

        try {
            paymentService.processWebhook(signature, payload);
            return ResponseEntity.ok("Webhook processed");
        } catch (Exception e) {
            log.error("Webhook processing failed", e);
            return ResponseEntity.badRequest().body("Webhook processing failed");
        }
    }
}

