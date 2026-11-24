package service;

import model.AvailabilityStatus;
import model.BedType;
import model.Customer;
import model.Invoice;
import model.Reservation;
import model.ReservationStatus;
import model.Room;
import model.RoomFactory;
import model.RoomType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 test suite validating the 5-Stage Invoice Calculation Algorithm,
 * room-type tax rates, 12% long-stay discount (>= 5 nights), extra guest charges,
 * and refundable advance deduction.
 */
class InvoiceServiceTest {

    private InvoiceService invoiceService;
    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(null);
        sampleCustomer = new Customer(1, "199512345678", "Kasun Perera", "+94771234567", "kasun@example.lk");
    }

    @Test
    @DisplayName("TC-INV-01: Standard Room, 3 nights, 2 guests (within capacity), no discount, 10% tax")
    void testStandardRoomShortStayNoExtraGuests() {
        Room room = RoomFactory.createRoom("R101", RoomType.STANDARD, BedType.DOUBLE, AvailabilityStatus.AVAILABLE);
        Reservation res = new Reservation("BK001", sampleCustomer, room,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 8),
                2, Reservation.STANDARD_BOOKING_ADVANCE, ReservationStatus.CONFIRMED);

        Invoice inv = invoiceService.calculateInvoice(res);

        // Base = 6000 * 3 = 18000 | Extra = 0 | Discount = 0 | Tax (10%) = 1800 | Gross = 19800 | Final = 9800
        assertEquals(3, inv.getNumberOfNights());
        assertEquals(18000.00, inv.getBaseRoomCharge(), 0.01);
        assertEquals(0.00, inv.getExtraGuestCharge(), 0.01);
        assertEquals(0.00, inv.getDiscountAmount(), 0.01);
        assertEquals(1800.00, inv.getTaxAmount(), 0.01);
        assertEquals(19800.00, inv.getGrossTotal(), 0.01);
        assertEquals(10000.00, inv.getBookingAdvanceDeducted(), 0.01);
        assertEquals(9800.00, inv.getFinalPayableAmount(), 0.01);
        assertEquals(0.00, inv.getRefundDue(), 0.01);
    }

    @Test
    @DisplayName("TC-INV-02: Deluxe Room, 5 nights (triggers 12% discount), 3 guests (1 extra guest), 12% tax")
    void testDeluxeRoomLongStayWithExtraGuest() {
        Room room = RoomFactory.createRoom("R201", RoomType.DELUXE, BedType.QUEEN, AvailabilityStatus.AVAILABLE);
        Reservation res = new Reservation("BK002", sampleCustomer, room,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 15),
                3, Reservation.STANDARD_BOOKING_ADVANCE, ReservationStatus.CONFIRMED);

        Invoice inv = invoiceService.calculateInvoice(res);

        // Base = 9000 * 5 = 45000
        // Extra = 1 guest * 1500 * 5 nights = 7500
        // Discount = 12% of Base (45000) = 5400
        // Taxable = (45000 - 5400) + 7500 = 47100
        // Tax (12%) = 5652
        // Gross = 52752 | Final = 42752
        assertEquals(5, inv.getNumberOfNights());
        assertEquals(1, inv.getExtraGuestsCount());
        assertEquals(45000.00, inv.getBaseRoomCharge(), 0.01);
        assertEquals(7500.00, inv.getExtraGuestCharge(), 0.01);
        assertEquals(5400.00, inv.getDiscountAmount(), 0.01);
        assertEquals(5652.00, inv.getTaxAmount(), 0.01);
        assertEquals(52752.00, inv.getGrossTotal(), 0.01);
        assertEquals(42752.00, inv.getFinalPayableAmount(), 0.01);
    }

    @Test
    @DisplayName("TC-INV-03: Standard Room, 1 night -> Gross < LKR 10,000 advance yields Refund Due")
    void testShortStayRefundDueWhenGrossBelowAdvance() {
        Room room = RoomFactory.createRoom("R102", RoomType.STANDARD, BedType.SINGLE, AvailabilityStatus.AVAILABLE);
        Reservation res = new Reservation("BK003", sampleCustomer, room,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 4), LocalDate.of(2026, 12, 5),
                1, Reservation.STANDARD_BOOKING_ADVANCE, ReservationStatus.CONFIRMED);

        Invoice inv = invoiceService.calculateInvoice(res);

        // Base = 6000 | Tax (10%) = 600 | Gross = 6600 | Advance = 10000 -> Payable = 0, Refund Due = 3400
        assertEquals(6600.00, inv.getGrossTotal(), 0.01);
        assertEquals(0.00, inv.getFinalPayableAmount(), 0.01);
        assertEquals(3400.00, inv.getRefundDue(), 0.01);
    }

    @Test
    @DisplayName("TC-INV-04: Family Suite, 4 nights (boundary below 5 -> no discount), 6 guests (2 extra), 15% tax")
    void testFamilySuiteBoundaryFourNightsNoDiscount() {
        Room room = RoomFactory.createRoom("R401", RoomType.FAMILY_SUITE, BedType.KING, AvailabilityStatus.AVAILABLE);
        Reservation res = new Reservation("BK004", sampleCustomer, room,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 9),
                6, Reservation.STANDARD_BOOKING_ADVANCE, ReservationStatus.CONFIRMED);

        Invoice inv = invoiceService.calculateInvoice(res);

        // Base = 18000 * 4 = 72000 | Extra = 2 * 2500 * 4 = 20000 | Discount = 0 | Taxable = 92000 | Tax (15%) = 13800
        // Gross = 105800 | Final = 95800
        assertEquals(0.00, inv.getDiscountAmount(), 0.01);
        assertEquals(20000.00, inv.getExtraGuestCharge(), 0.01);
        assertEquals(13800.00, inv.getTaxAmount(), 0.01);
        assertEquals(95800.00, inv.getFinalPayableAmount(), 0.01);

        String formatted = invoiceService.formatInvoiceText(inv);
        assertTrue(formatted.contains("R401"));
        assertTrue(formatted.contains("Family Suite"));
        assertTrue(formatted.contains("Kasun Perera"));
        assertTrue(formatted.contains("FINAL PAYABLE AMOUNT"));
    }
}
