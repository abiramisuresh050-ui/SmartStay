package service;

import model.AvailabilityStatus;
import model.BedType;
import model.Customer;
import model.Reservation;
import model.ReservationStatus;
import model.Room;
import model.RoomFactory;
import model.RoomType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 test suite verifying Booking Business Rules (2-day advance, 24-hour freeze,
 * availability guard, date-overlap detection) and explicit Algorithms (Merge Sort,
 * Binary Search, Linear Search).
 */
class BookingServiceTest {

    private BookingService bookingService;
    private Customer alice;
    private Customer bob;
    private Room standardRoom;
    private Room suiteRoom;
    private Room maintenanceRoom;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(null, null, null);
        alice = new Customer(1, "199812345678", "Alice Fernando", "+94711111111", "alice@example.lk");
        bob = new Customer(2, "N5566778", "Bob Silva", "+94722222222", "bob@example.lk");
        standardRoom = RoomFactory.createRoom("R101", RoomType.STANDARD, BedType.DOUBLE, AvailabilityStatus.AVAILABLE);
        suiteRoom = RoomFactory.createRoom("R301", RoomType.SUITE, BedType.KING, AvailabilityStatus.AVAILABLE);
        maintenanceRoom = RoomFactory.createRoom("R402", RoomType.FAMILY_SUITE, BedType.QUEEN, AvailabilityStatus.UNDER_MAINTENANCE);
    }

    @Test
    @DisplayName("TC-BK-01: Enforce 2-day advance booking rule (reject 1 day ahead, accept 2 days ahead)")
    void testTwoDayAdvanceBookingRule() throws SQLException {
        LocalDate today = LocalDate.of(2026, 12, 1);

        // 1 day ahead -> must throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                bookingService.createReservation(alice, standardRoom, 2, today, today.plusDays(1), today.plusDays(3)));

        // Exactly 2 days ahead -> valid
        Reservation created = bookingService.createReservation(alice, standardRoom, 2, today, today.plusDays(2), today.plusDays(4));
        assertNotNull(created);
        assertEquals(Reservation.STANDARD_BOOKING_ADVANCE, created.getBookingAdvance(), 0.01);
        assertEquals(AvailabilityStatus.RESERVED, standardRoom.getAvailabilityStatus());
    }

    @Test
    @DisplayName("TC-BK-02: Block reservation when room status is Under Maintenance or Reserved")
    void testUnavailableRoomCannotBeBooked() {
        LocalDate today = LocalDate.of(2026, 12, 1);
        assertThrows(IllegalStateException.class, () ->
                bookingService.createReservation(alice, maintenanceRoom, 2, today, today.plusDays(3), today.plusDays(5)));
    }

    @Test
    @DisplayName("TC-BK-03: Enforce 24-hour cancellation & modification freeze window")
    void testTwentyFourHourCancellationFreeze() throws SQLException {
        LocalDate bookingDate = LocalDate.of(2026, 12, 1);
        LocalDate checkIn = LocalDate.of(2026, 12, 10);
        Reservation res = bookingService.createReservation(alice, suiteRoom, 2, bookingDate, checkIn, checkIn.plusDays(2));

        // 10 hours before check-in (Dec 10 04:00 vs 14:00 check-in) -> NOT eligible
        LocalDateTime tenHoursBefore = LocalDateTime.of(2026, 12, 10, 4, 0);
        assertFalse(bookingService.isEligibleForModificationOrCancellation(res, tenHoursBefore));
        assertThrows(IllegalStateException.class, () -> bookingService.cancelReservation(res, tenHoursBefore));

        // 30 hours before check-in (Dec 9 08:00 vs Dec 10 14:00) -> Eligible
        LocalDateTime thirtyHoursBefore = LocalDateTime.of(2026, 12, 9, 8, 0);
        assertTrue(bookingService.isEligibleForModificationOrCancellation(res, thirtyHoursBefore));
        bookingService.cancelReservation(res, thirtyHoursBefore);
        assertEquals(ReservationStatus.CANCELLED, res.getStatus());
        assertEquals(AvailabilityStatus.AVAILABLE, suiteRoom.getAvailabilityStatus());
    }

    @Test
    @DisplayName("TC-ALG-01: Merge Sort by Check-in Date orders reservations chronologically")
    void testMergeSortByCheckInDate() {
        Reservation r1 = new Reservation("BK003", alice, standardRoom, LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 22), 2, 10000, ReservationStatus.CONFIRMED);
        Reservation r2 = new Reservation("BK001", bob, suiteRoom, LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 7), 2, 10000, ReservationStatus.CONFIRMED);
        Reservation r3 = new Reservation("BK002", alice, suiteRoom, LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 14), 3, 10000, ReservationStatus.CONFIRMED);

        List<Reservation> sorted = bookingService.mergeSortByCheckInDate(Arrays.asList(r1, r2, r3));

        assertEquals("BK001", sorted.get(0).getReservationId());
        assertEquals("BK002", sorted.get(1).getReservationId());
        assertEquals("BK003", sorted.get(2).getReservationId());
    }

    @Test
    @DisplayName("TC-ALG-02: Binary Search by Booking ID finds exact match and returns null when absent")
    void testBinarySearchByBookingId() {
        Reservation r1 = new Reservation("BK010", alice, standardRoom, LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12), 2, 10000, ReservationStatus.CONFIRMED);
        Reservation r2 = new Reservation("BK002", bob, suiteRoom, LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 17), 2, 10000, ReservationStatus.CONFIRMED);
        Reservation r3 = new Reservation("BK005", alice, suiteRoom, LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 20), LocalDate.of(2026, 12, 22), 2, 10000, ReservationStatus.CONFIRMED);

        List<Reservation> unsorted = Arrays.asList(r1, r2, r3);

        Reservation found = bookingService.binarySearchByBookingId(unsorted, "BK005");
        assertNotNull(found);
        assertEquals("BK005", found.getReservationId());

        assertNull(bookingService.binarySearchByBookingId(unsorted, "BK999"));
    }

    @Test
    @DisplayName("TC-ALG-03: Linear Search by Guest Name matches case-insensitive substrings")
    void testLinearSearchByGuestName() {
        Reservation r1 = new Reservation("BK001", alice, standardRoom, LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 12), 2, 10000, ReservationStatus.CONFIRMED);
        Reservation r2 = new Reservation("BK002", bob, suiteRoom, LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 17), 2, 10000, ReservationStatus.CONFIRMED);

        List<Reservation> matches = bookingService.linearSearchByGuestName(Arrays.asList(r1, r2), "fernando");
        assertEquals(1, matches.size());
        assertEquals("BK001", matches.get(0).getReservationId());
    }

    @Test
    @DisplayName("TC-ALG-04: Date-Interval Overlap Detection identifies overlapping vs adjacent stays")
    void testDateOverlapDetection() {
        LocalDate existingIn = LocalDate.of(2026, 12, 10);
        LocalDate existingOut = LocalDate.of(2026, 12, 15);

        // Overlapping stay (Dec 12 - Dec 18)
        assertTrue(bookingService.hasDateOverlap(LocalDate.of(2026, 12, 12), LocalDate.of(2026, 12, 18), existingIn, existingOut));

        // Adjacent non-overlapping stay (Dec 15 - Dec 17)
        assertFalse(bookingService.hasDateOverlap(LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 17), existingIn, existingOut));
    }
}
