package com.blackmagic.BlackMagic.services;

import com.blackmagic.BlackMagic.dtos.publicDtos.SessionResponse;
import com.blackmagic.BlackMagic.dtos.publicDtos.SessionStartRequest;
import com.blackmagic.BlackMagic.exception.*;
import com.blackmagic.BlackMagic.models.*;
import com.blackmagic.BlackMagic.repos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
@Service
@RequiredArgsConstructor
@Slf4j
public class TableSessionService {
    private final TableRepository tableRepository;
    private final TableSessionRepository sessionRepository;

    @Transactional
    public SessionResponse startSession(SessionStartRequest request) {
        // Validate QR token
        Table table = tableRepository.findByQrToken(request.getQrToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid QR code"));

        if (!table.getActive()) {
            throw new BusinessException("Table is not active");
        }

        // Check if there's already an active session
        Optional<TableSession> existingSession = sessionRepository
                .findActiveSessionByTableId(table.getId());

        if (existingSession.isPresent()) {
            TableSession session = existingSession.get();
            // Add device to existing session
            if (request.getDeviceId() != null) {
                List<String> devices = session.getDeviceIds();
                if (devices == null) {
                    devices = new ArrayList<>();
                }
                if (!devices.contains(request.getDeviceId())) {
                    devices.add(request.getDeviceId());
                    session.setDeviceIds(devices);
                }
            }
            session.setLastActivityAt(LocalDateTime.now());
            if (request.getPartySize() != null) {
                session.setActiveCustomers(request.getPartySize());
            }
            sessionRepository.save(session);

            return buildSessionResponse(table, session);
        }

        // Create new session
        String sessionCode = generateSessionCode();
        List<String> deviceIds = new ArrayList<>();
        if (request.getDeviceId() != null) {
            deviceIds.add(request.getDeviceId());
        }

        TableSession session = TableSession.builder()
                .tableId(table.getId())
                .sessionCode(sessionCode)
                .status(TableSession.SessionStatus.ACTIVE)
                .activeCustomers(request.getPartySize())
                .deviceIds(deviceIds)
                .startedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build();

        sessionRepository.save(session);

        log.info("Started session {} for table {}", sessionCode, table.getTableNumber());

        return buildSessionResponse(table, session);
    }

    @Transactional
    public void closeSession(String sessionCode) {
        TableSession session = sessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        session.setStatus(TableSession.SessionStatus.COMPLETED);
        session.setClosedAt(LocalDateTime.now());
        sessionRepository.save(session);

        log.info("Closed session {}", sessionCode);
    }

    public TableSession getActiveSession(String sessionCode) {
        TableSession session = sessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (session.getStatus() != TableSession.SessionStatus.ACTIVE) {
            throw new BusinessException("Session is not active");
        }

        // Update last activity
        session.setLastActivityAt(LocalDateTime.now());
        sessionRepository.save(session);

        return session;
    }

    private String generateSessionCode() {
        return "SES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private SessionResponse buildSessionResponse(Table table, TableSession session) {
        return SessionResponse.builder()
                .sessionCode(session.getSessionCode())
                .tableNumber(table.getTableNumber())
                .status(session.getStatus().name())
                .startedAt(session.getStartedAt())
                .build();
    }
}