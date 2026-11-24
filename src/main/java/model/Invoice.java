package model;

import java.time.LocalDateTime;

/**
 * Represents a detailed invoice generated for a {@link Reservation} (Composition relationship).
 * Holds every itemized component required by the SmartStay specification.
 */
public class Invoice {

    private String invoiceId;
    private Reservation reservation;
    private long numberOfNights;
    private int extraGuestsCount;
    private double baseRoomCharge;
    private double extraGuestCharge;
    private double discountAmount;
    private double taxRate;
    private double taxAmount;
    private double grossTotal;
    private double bookingAdvanceDeducted;
    private double finalPayableAmount;
    private double refundDue;
    private LocalDateTime generatedAt;

    public Invoice(String invoiceId, Reservation reservation, long numberOfNights, int extraGuestsCount,
                   double baseRoomCharge, double extraGuestCharge, double discountAmount,
                   double taxRate, double taxAmount, double grossTotal,
                   double bookingAdvanceDeducted, double finalPayableAmount, double refundDue,
                   LocalDateTime generatedAt) {
        this.invoiceId = invoiceId;
        this.reservation = reservation;
        this.numberOfNights = numberOfNights;
        this.extraGuestsCount = extraGuestsCount;
        this.baseRoomCharge = baseRoomCharge;
        this.extraGuestCharge = extraGuestCharge;
        this.discountAmount = discountAmount;
        this.taxRate = taxRate;
        this.taxAmount = taxAmount;
        this.grossTotal = grossTotal;
        this.bookingAdvanceDeducted = bookingAdvanceDeducted;
        this.finalPayableAmount = finalPayableAmount;
        this.refundDue = refundDue;
        this.generatedAt = generatedAt;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public long getNumberOfNights() {
        return numberOfNights;
    }

    public int getExtraGuestsCount() {
        return extraGuestsCount;
    }

    public double getBaseRoomCharge() {
        return baseRoomCharge;
    }

    public double getExtraGuestCharge() {
        return extraGuestCharge;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public double getGrossTotal() {
        return grossTotal;
    }

    public double getBookingAdvanceDeducted() {
        return bookingAdvanceDeducted;
    }

    public double getFinalPayableAmount() {
        return finalPayableAmount;
    }

    public double getRefundDue() {
        return refundDue;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
}
