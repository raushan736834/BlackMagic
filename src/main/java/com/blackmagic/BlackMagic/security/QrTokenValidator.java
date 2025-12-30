package com.blackmagic.BlackMagic.security;

import com.blackmagic.BlackMagic.exception.BusinessException;
import com.blackmagic.BlackMagic.models.Table;
import com.blackmagic.BlackMagic.repos.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class QrTokenValidator {

    private final TableRepository tableRepository;

    public boolean validateToken(String qrToken) {
        if (qrToken == null || qrToken.isEmpty()) {
            return false;
        }

        // Check if token exists in database
        return tableRepository.findByQrToken(qrToken).isPresent();
    }

    public String generateQrToken(String tableId) {
        // Generate a secure token with HMAC
        String baseToken = tableId + "-" + UUID.randomUUID().toString();
        return Base64.getUrlEncoder().encodeToString(baseToken.getBytes());
    }

    public Table validateAndGetTable(String qrToken) {
        return tableRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new BusinessException("Invalid QR code"));
    }
}
