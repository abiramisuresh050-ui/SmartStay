package service;

import dao.ReservationDAO;
import model.Customer;
import model.Invoice;
import model.Reservation;
import model.Room;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Business logic service implementing the SmartStay 5-Stage Final Bill Calculation Algorithm
 * and detailed Invoice generation.
 */
public class InvoiceService {

    /** Long-stay discount threshold: 5 or more nights receive a 12% discount on base room charge. */
    public static final int LONG_STAY_NIGHTS_THRESHOLD = 5;
    public static final double LONG_STAY_DISCOUNT_RATE = 0.12;

    private final ReservationDAO reservationDAO;

    public InvoiceService() {
        this(new ReservationDAO());
    }

    public InvoiceService(ReservationDAO reservationDAO) {
        this.reservationDAO = reservationDAO;
    }

    /**
     * Executes the exact 5-step SmartStay Final Bill Calculation Algorithm:
     * <ol>
     *   <li>Base Room Charge = Daily Rate * Number of Nights</li>
     *   <li>Extra Guest Charge = max(0, Guests - BaseCapacity) * ExtraGuestRate * Nights [ASSUMPTION-02]</li>
     *   <li>Long-Stay Discount = 12% of Base Room Charge if Nights >= 5 (before tax)</li>
     *   <li>Tax = ((Base Room Charge - Discount) + Extra Guest Charge) * Room Tax Rate [ASSUMPTION-03]</li>
     *   <li>Deduct Refundable Booking Advance (LKR 10,000); compute Refund Due if Gross Total < Advance [ASSUMPTION-04]</li>
     * </ol>
     *
     * @param reservation the reservation to invoice
     * @return itemized {@link Invoice} object
     */
    public Invoice calculateInvoice(Reservation reservation) {
        if (reservation == null || reservation.getRoom() == null || reservation.getCustomer() == null) {
            throw new IllegalArgumentException("Reservation, room, and customer details are required to generate an invoice.");
        }

        Room room = reservation.getRoom();
        long numNights = reservation.getNumberOfNights();
        if (numNights <= 0) {
            throw new IllegalArgumentException("Number of nights must be at least 1.");
        }

        // Step 1: Base room charge = Daily Rate * Number of Nights
        double baseRoomCharge = room.getDailyRate() * numNights;

        // Step 2: Extra guest charges if number of guests exceeds base room capacity (per extra guest per night)
        int extraGuests = Math.max(0, reservation.getNumberOfGuests() - room.getBaseCapacity());
        double extraGuestCharge = extraGuests * room.getExtraGuestChargeRate() * numNights;

        // Step 3: Apply 12% discount on base room charge (before tax) if staying 5 or more nights
        double discountAmount = (numNights >= LONG_STAY_NIGHTS_THRESHOLD)
                ? roundCurrency(baseRoomCharge * LONG_STAY_DISCOUNT_RATE)
                : 0.00;

        // Step 4: Add tax according to room type on net taxable subtotal
        double taxableSubtotal = (baseRoomCharge - discountAmount) + extraGuestCharge;
        double taxRate = room.getTaxRate();
        double taxAmount = roundCurrency(taxableSubtotal * taxRate);

        double grossTotal = roundCurrency(taxableSubtotal + taxAmount);

        // Step 5: Deduct refundable booking advance (LKR 10,000)
        double advanceDeducted = reservation.getBookingAdvance();
        double netBalance = roundCurrency(grossTotal - advanceDeducted);

        double finalPayableAmount = Math.max(0.00, netBalance);
        double refundDue = (netBalance < 0) ? Math.abs(netBalance) : 0.00;

        String invoiceId = "INV-" + reservation.getReservationId();
        return new Invoice(
                invoiceId,
                reservation,
                numNights,
                extraGuests,
                roundCurrency(baseRoomCharge),
                roundCurrency(extraGuestCharge),
                discountAmount,
                taxRate,
                taxAmount,
                grossTotal,
                roundCurrency(advanceDeducted),
                roundCurrency(finalPayableAmount),
                roundCurrency(refundDue),
                LocalDateTime.now()
        );
    }

    /**
     * Generates and persists the invoice to MySQL.
     */
    public Invoice generateAndSaveInvoice(Reservation reservation) throws SQLException {
        Invoice invoice = calculateInvoice(reservation);
        if (reservationDAO != null) {
            reservationDAO.saveInvoice(invoice);
        }
        return invoice;
    }

    /**
     * Records final payment settlement in MySQL and marks the reservation COMPLETED.
     */
    public void settleInvoice(Invoice invoice) throws SQLException {
        if (reservationDAO != null) {
            reservationDAO.saveInvoice(invoice);
            reservationDAO.recordFinalPayment(invoice.getReservation(), invoice.getFinalPayableAmount());
        }
    }

    /**
     * Formats a detailed, itemized text receipt containing every required invoice field.
     */
    public String formatInvoiceText(Invoice invoice) {
        DecimalFormat money = new DecimalFormat("#,##0.00");
        DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        Reservation res = invoice.getReservation();
        Customer guest = res.getCustomer();
        Room room = res.getRoom();

        StringBuilder sb = new StringBuilder();
        sb.append("============================================================\n");
        sb.append("              SMARTSTAY HOTEL BOOKING SYSTEM                \n");
        sb.append("                     DETAILED INVOICE                       \n");
        sb.append("============================================================\n");
        sb.append(String.format("Invoice ID      : %s%n", invoice.getInvoiceId()));
        sb.append(String.format("Booking ID      : %s%n", res.getReservationId()));
        sb.append(String.format("Generated At    : %s%n", invoice.getGeneratedAt().format(dtFmt)));
        sb.append("------------------------------------------------------------\n");
        sb.append("GUEST DETAILS\n");
        sb.append(String.format("Full Name       : %s%n", guest.getFullName()));
        sb.append(String.format("NIC / Passport  : %s%n", guest.getNicPassport()));
        sb.append(String.format("Contact Number  : %s%n", guest.getContactNumber()));
        sb.append(String.format("Email Address   : %s%n", guest.getEmail()));
        sb.append("------------------------------------------------------------\n");
        sb.append("ROOM & STAY DETAILS\n");
        sb.append(String.format("Room ID         : %s%n", room.getRoomId()));
        sb.append(String.format("Room Type       : %s%n", room.getRoomType().getDisplayName()));
        sb.append(String.format("Bed Type        : %s%n", room.getBedType().getDisplayName()));
        sb.append(String.format("Daily Room Rate : LKR %s%n", money.format(room.getDailyRate())));
        sb.append(String.format("Breakfast Incl. : %s%n", room.hasComplimentaryBreakfast() ? "Yes (Complimentary)" : "No"));
        sb.append(String.format("Check-in Date   : %s%n", res.getCheckInDate()));
        sb.append(String.format("Check-out Date  : %s%n", res.getCheckOutDate()));
        sb.append(String.format("Number of Nights: %d%n", invoice.getNumberOfNights()));
        sb.append(String.format("Number of Guests: %d (Base Capacity: %d | Extra: %d)%n",
                res.getNumberOfGuests(), room.getBaseCapacity(), invoice.getExtraGuestsCount()));
        sb.append("------------------------------------------------------------\n");
        sb.append("BILLING BREAKDOWN\n");
        sb.append(String.format("%-36s LKR %14s%n", "Base Room Charge:", money.format(invoice.getBaseRoomCharge())));
        sb.append(String.format("%-36s LKR %14s%n", "Extra Guest Charges:", money.format(invoice.getExtraGuestCharge())));
        sb.append(String.format("%-36s-LKR %14s%n", "Long-Stay Discount (12% >=5 nts):", money.format(invoice.getDiscountAmount())));
        sb.append(String.format("%-36s LKR %14s%n",
                String.format("Tax (%.0f%%):", invoice.getTaxRate() * 100), money.format(invoice.getTaxAmount())));
        sb.append("------------------------------------------------------------\n");
        sb.append(String.format("%-36s LKR %14s%n", "Gross Total:", money.format(invoice.getGrossTotal())));
        sb.append(String.format("%-36s-LKR %14s%n", "Refundable Booking Advance:", money.format(invoice.getBookingAdvanceDeducted())));
        sb.append("============================================================\n");
        sb.append(String.format("%-36s LKR %14s%n", "FINAL PAYABLE AMOUNT:", money.format(invoice.getFinalPayableAmount())));
        if (invoice.getRefundDue() > 0) {
            sb.append(String.format("%-36s LKR %14s%n", "REFUND DUE TO GUEST:", money.format(invoice.getRefundDue())));
        }
        sb.append("============================================================\n");
        return sb.toString();
    }

    private double roundCurrency(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
