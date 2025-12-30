package com.blackmagic.BlackMagic.scheduler;

import com.blackmagic.BlackMagic.models.*;
import com.blackmagic.BlackMagic.repos.*;
import com.blackmagic.BlackMagic.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class BookingReminderScheduler {

    private final TableBookingRepository bookingRepository;
    private final NotificationService notificationService;

    /**
     * Send reminders for today's bookings every hour
     */
    @Scheduled(cron = "0 0 * * * ?") // Every hour
    @Transactional
    public void sendBookingReminders() {
        log.info("Sending booking reminders...");

        LocalDate today = LocalDate.now();

        List<TableBooking> todayBookings = bookingRepository
                .findByBookingDateAndStatus(today, TableBooking.BookingStatus.CONFIRMED);

        int remindersSent = 0;

        for (TableBooking booking : todayBookings) {
            // Send reminder 2 hours before booking time
            // Parse timeSlot and check if within 2 hours
            try {
                notificationService.sendBookingReminder(booking);
                remindersSent++;
            } catch (Exception e) {
                log.error("Failed to send reminder for booking {}", booking.getId(), e);
            }
        }

        log.info("Sent {} booking reminders", remindersSent);
    }

    /**
     * Mark no-shows for bookings
     */
    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
    @Transactional
    public void markNoShows() {
        log.info("Marking no-show bookings...");

        LocalDate yesterday = LocalDate.now().minusDays(1);

        List<TableBooking> confirmedBookings = bookingRepository
                .findByBookingDateAndStatus(yesterday, TableBooking.BookingStatus.CONFIRMED);

        for (TableBooking booking : confirmedBookings) {
            booking.setStatus(TableBooking.BookingStatus.NO_SHOW);
            bookingRepository.save(booking);
        }

        log.info("Marked {} bookings as no-show", confirmedBookings.size());
    }
}
