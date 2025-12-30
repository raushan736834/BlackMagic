package com.blackmagic.BlackMagic.scheduler;

import com.blackmagic.BlackMagic.models.*;
import com.blackmagic.BlackMagic.repos.*;
import com.blackmagic.BlackMagic.services.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class SessionCleanupScheduler {

    private final TableSessionRepository sessionRepository;

    @Value("${session.inactive.timeout.minutes}")
    private Integer inactiveTimeoutMinutes;

    @Value("${session.auto.close.hours}")
    private Integer autoCloseHours;

    /**
     * Close inactive sessions every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    @Transactional
    public void closeInactiveSessions() {
        log.info("Running inactive session cleanup...");

        LocalDateTime inactiveThreshold = LocalDateTime.now()
                .minusMinutes(inactiveTimeoutMinutes);

        List<TableSession> inactiveSessions = sessionRepository
                .findByStatusAndLastActivityAtBefore(
                        TableSession.SessionStatus.ACTIVE,
                        inactiveThreshold
                );

        for (TableSession session : inactiveSessions) {
            session.setStatus(TableSession.SessionStatus.EXPIRED);
            session.setClosedAt(LocalDateTime.now());
            sessionRepository.save(session);

            log.info("Closed inactive session: {}", session.getSessionCode());
        }

        log.info("Closed {} inactive sessions", inactiveSessions.size());
    }

    /**
     * Auto-close very old active sessions (safety net)
     */
    @Scheduled(cron = "0 0 */4 * * ?") // Every 4 hours
    @Transactional
    public void closeOldSessions() {
        log.info("Running old session cleanup...");

        LocalDateTime oldThreshold = LocalDateTime.now()
                .minusHours(autoCloseHours);

        List<TableSession> oldSessions = sessionRepository
                .findByStatusAndLastActivityAtBefore(
                        TableSession.SessionStatus.ACTIVE,
                        oldThreshold
                );

        for (TableSession session : oldSessions) {
            session.setStatus(TableSession.SessionStatus.COMPLETED);
            session.setClosedAt(LocalDateTime.now());
            sessionRepository.save(session);

            log.warn("Force-closed old session: {} (started at {})",
                    session.getSessionCode(), session.getStartedAt());
        }

        log.info("Force-closed {} old sessions", oldSessions.size());
    }
}
