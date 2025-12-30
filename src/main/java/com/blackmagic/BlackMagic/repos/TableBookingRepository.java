package com.blackmagic.BlackMagic.repos;

import com.blackmagic.BlackMagic.models.TableBooking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TableBookingRepository extends MongoRepository<TableBooking, String> {
    List<TableBooking> findByBookingDateAndStatus(
            LocalDate date,
            TableBooking.BookingStatus status
    );

    List<TableBooking> findByTableIdAndBookingDate(String tableId, LocalDate date);

    @Query("{ 'bookingDate': ?0, 'timeSlot': ?1, 'status': { $in: ['CONFIRMED', 'PENDING'] } }")
    List<TableBooking> findConflictingBookings(LocalDate date, String timeSlot);

    List<TableBooking> findByMobileOrderByCreatedAtDesc(String mobile);

    List<TableBooking> findByStatusAndBookingDateBefore(
            TableBooking.BookingStatus status,
            LocalDate date
    );
}
