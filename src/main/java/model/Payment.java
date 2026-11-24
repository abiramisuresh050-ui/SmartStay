package model;

import java.time.LocalDateTime;

/**
 * Represents a financial payment or refund event linked to a {@link Reservation}.
 */
public class Payment {

    public enum PaymentType {
        ADVANCE,
        FINAL_SETTLEMENT,
        REFUND
    }

    private int paymentId;
    private String reservationId;
    private PaymentType paymentType;
    private double amount;
    private LocalDateTime paymentDate;

    public Payment(int paymentId, String reservationId, PaymentType paymentType, double amount, LocalDateTime paymentDate) {
        this.paymentId = paymentId;
        this.reservationId = reservationId;
        this.paymentType = paymentType;
        this.amount = amount;
        this.paymentDate = paymentDate;
    }

    public Payment(String reservationId, PaymentType paymentType, double amount) {
        this(0, reservationId, paymentType, amount, LocalDateTime.now());
    }

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }
}
