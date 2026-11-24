package model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Represents a room reservation associating a {@link Customer} with a {@link Room}.
 * Enforces the refundable LKR 10,000 booking advance and stores check-in/check-out dates.
 */
public class Reservation {

    public static final double STANDARD_BOOKING_ADVANCE = 10000.00;

    private String reservationId;
    private Customer customer;
    private Room room;
    private LocalDate bookingDate;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int numberOfGuests;
    private double bookingAdvance;
    private ReservationStatus status;

    public Reservation(String reservationId, Customer customer, Room room,
                       LocalDate bookingDate, LocalDate checkInDate, LocalDate checkOutDate,
                       int numberOfGuests, double bookingAdvance, ReservationStatus status) {
        this.reservationId = reservationId;
        this.customer = customer;
        this.room = room;
        this.bookingDate = bookingDate;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.numberOfGuests = numberOfGuests;
        this.bookingAdvance = bookingAdvance;
        this.status = status;
    }

    public long getNumberOfNights() {
        return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    public void setNumberOfGuests(int numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }

    public double getBookingAdvance() {
        return bookingAdvance;
    }

    public void setBookingAdvance(double bookingAdvance) {
        this.bookingAdvance = bookingAdvance;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return reservationId + " | " + (customer != null ? customer.getFullName() : "Unknown")
                + " | " + (room != null ? room.getRoomId() : "N/A")
                + " | " + checkInDate + " to " + checkOutDate;
    }
}
