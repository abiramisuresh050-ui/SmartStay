package dao;

import database.DatabaseConnection;
import model.AvailabilityStatus;
import model.BedType;
import model.Customer;
import model.Invoice;
import model.Payment;
import model.Reservation;
import model.ReservationStatus;
import model.Room;
import model.RoomFactory;
import model.RoomType;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for managing {@link Reservation}, {@link Invoice},
 * and {@link Payment} persistence in MySQL via JDBC.
 */
public class ReservationDAO {

    /**
     * Persists a new reservation along with its mandatory LKR 10,000 advance payment
     * and updates the room availability status inside a single database transaction.
     */
    public boolean createReservation(Reservation reservation) throws SQLException {
        String insertResSql = "INSERT INTO reservations (reservation_id, customer_id, room_id, booking_date, "
                + "check_in_date, check_out_date, num_guests, booking_advance, reservation_status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertPaySql = "INSERT INTO payments (reservation_id, payment_type, amount) VALUES (?, ?, ?)";
        String updateRoomSql = "UPDATE rooms SET availability_status = ? WHERE room_id = ?";

        Connection conn = DatabaseConnection.getInstance().getConnection();
        try {
            conn.setAutoCommit(false);

            try (PreparedStatement resStmt = conn.prepareStatement(insertResSql)) {
                resStmt.setString(1, reservation.getReservationId());
                resStmt.setInt(2, reservation.getCustomer().getCustomerId());
                resStmt.setString(3, reservation.getRoom().getRoomId());
                resStmt.setDate(4, Date.valueOf(reservation.getBookingDate()));
                resStmt.setDate(5, Date.valueOf(reservation.getCheckInDate()));
                resStmt.setDate(6, Date.valueOf(reservation.getCheckOutDate()));
                resStmt.setInt(7, reservation.getNumberOfGuests());
                resStmt.setDouble(8, reservation.getBookingAdvance());
                resStmt.setString(9, reservation.getStatus().name());
                resStmt.executeUpdate();
            }

            try (PreparedStatement payStmt = conn.prepareStatement(insertPaySql)) {
                payStmt.setString(1, reservation.getReservationId());
                payStmt.setString(2, Payment.PaymentType.ADVANCE.name());
                payStmt.setDouble(3, reservation.getBookingAdvance());
                payStmt.executeUpdate();
            }

            try (PreparedStatement roomStmt = conn.prepareStatement(updateRoomSql)) {
                roomStmt.setString(1, AvailabilityStatus.RESERVED.name());
                roomStmt.setString(2, reservation.getRoom().getRoomId());
                roomStmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    /**
     * Updates an existing reservation's dates and guest count.
     */
    public boolean updateReservation(Reservation reservation) throws SQLException {
        String sql = "UPDATE reservations SET check_in_date = ?, check_out_date = ?, num_guests = ?, "
                + "reservation_status = ? WHERE reservation_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(reservation.getCheckInDate()));
            stmt.setDate(2, Date.valueOf(reservation.getCheckOutDate()));
            stmt.setInt(3, reservation.getNumberOfGuests());
            stmt.setString(4, reservation.getStatus().name());
            stmt.setString(5, reservation.getReservationId());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Cancels a reservation, logs the refundable LKR 10,000 advance refund,
     * and restores the room status back to AVAILABLE within a transaction.
     */
    public boolean cancelReservation(Reservation reservation) throws SQLException {
        String updateResSql = "UPDATE reservations SET reservation_status = ? WHERE reservation_id = ?";
        String updateRoomSql = "UPDATE rooms SET availability_status = ? WHERE room_id = ?";
        String insertRefundSql = "INSERT INTO payments (reservation_id, payment_type, amount) VALUES (?, ?, ?)";

        Connection conn = DatabaseConnection.getInstance().getConnection();
        try {
            conn.setAutoCommit(false);

            try (PreparedStatement resStmt = conn.prepareStatement(updateResSql)) {
                resStmt.setString(1, ReservationStatus.CANCELLED.name());
                resStmt.setString(2, reservation.getReservationId());
                resStmt.executeUpdate();
            }

            try (PreparedStatement roomStmt = conn.prepareStatement(updateRoomSql)) {
                roomStmt.setString(1, AvailabilityStatus.AVAILABLE.name());
                roomStmt.setString(2, reservation.getRoom().getRoomId());
                roomStmt.executeUpdate();
            }

            try (PreparedStatement refundStmt = conn.prepareStatement(insertRefundSql)) {
                refundStmt.setString(1, reservation.getReservationId());
                refundStmt.setString(2, Payment.PaymentType.REFUND.name());
                refundStmt.setDouble(3, reservation.getBookingAdvance());
                refundStmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    /**
     * Persists or updates a generated {@link Invoice} record in MySQL.
     */
    public boolean saveInvoice(Invoice invoice) throws SQLException {
        String sql = "INSERT INTO invoices (invoice_id, reservation_id, num_nights, base_room_charge, "
                + "extra_guest_charge, discount_amount, tax_amount, advance_deducted, "
                + "final_payable_amount, refund_due, generated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE num_nights = VALUES(num_nights), "
                + "base_room_charge = VALUES(base_room_charge), extra_guest_charge = VALUES(extra_guest_charge), "
                + "discount_amount = VALUES(discount_amount), tax_amount = VALUES(tax_amount), "
                + "advance_deducted = VALUES(advance_deducted), final_payable_amount = VALUES(final_payable_amount), "
                + "refund_due = VALUES(refund_due), generated_at = VALUES(generated_at)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, invoice.getInvoiceId());
            stmt.setString(2, invoice.getReservation().getReservationId());
            stmt.setLong(3, invoice.getNumberOfNights());
            stmt.setDouble(4, invoice.getBaseRoomCharge());
            stmt.setDouble(5, invoice.getExtraGuestCharge());
            stmt.setDouble(6, invoice.getDiscountAmount());
            stmt.setDouble(7, invoice.getTaxAmount());
            stmt.setDouble(8, invoice.getBookingAdvanceDeducted());
            stmt.setDouble(9, invoice.getFinalPayableAmount());
            stmt.setDouble(10, invoice.getRefundDue());
            stmt.setTimestamp(11, Timestamp.valueOf(invoice.getGeneratedAt()));
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Records a payment (e.g., FINAL_SETTLEMENT) and marks reservation COMPLETED + room AVAILABLE.
     */
    public boolean recordFinalPayment(Reservation reservation, double amount) throws SQLException {
        String insertPaySql = "INSERT INTO payments (reservation_id, payment_type, amount) VALUES (?, ?, ?)";
        String updateResSql = "UPDATE reservations SET reservation_status = ? WHERE reservation_id = ?";
        String updateRoomSql = "UPDATE rooms SET availability_status = ? WHERE room_id = ?";

        Connection conn = DatabaseConnection.getInstance().getConnection();
        try {
            conn.setAutoCommit(false);

            try (PreparedStatement payStmt = conn.prepareStatement(insertPaySql)) {
                payStmt.setString(1, reservation.getReservationId());
                payStmt.setString(2, Payment.PaymentType.FINAL_SETTLEMENT.name());
                payStmt.setDouble(3, amount);
                payStmt.executeUpdate();
            }

            try (PreparedStatement resStmt = conn.prepareStatement(updateResSql)) {
                resStmt.setString(1, ReservationStatus.COMPLETED.name());
                resStmt.setString(2, reservation.getReservationId());
                resStmt.executeUpdate();
            }

            try (PreparedStatement roomStmt = conn.prepareStatement(updateRoomSql)) {
                roomStmt.setString(1, AvailabilityStatus.AVAILABLE.name());
                roomStmt.setString(2, reservation.getRoom().getRoomId());
                roomStmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
            conn.close();
        }
    }

    /**
     * Loads all reservations with joined Customer and Room objects into an {@link ArrayList}
     * so that in-memory Java sorting and searching algorithms can operate on them.
     */
    public List<Reservation> getAllReservations() throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT r.reservation_id, r.booking_date, r.check_in_date, r.check_out_date, "
                + "r.num_guests, r.booking_advance, r.reservation_status, "
                + "c.customer_id, c.nic_passport, c.full_name, c.contact_number, c.email, "
                + "rm.room_id, rm.room_type, rm.bed_type, rm.availability_status "
                + "FROM reservations r "
                + "JOIN customers c ON r.customer_id = c.customer_id "
                + "JOIN rooms rm ON r.room_id = rm.room_id";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Customer customer = new Customer(
                        rs.getInt("customer_id"),
                        rs.getString("nic_passport"),
                        rs.getString("full_name"),
                        rs.getString("contact_number"),
                        rs.getString("email")
                );

                Room room = RoomFactory.createRoom(
                        rs.getString("room_id"),
                        RoomType.valueOf(rs.getString("room_type")),
                        BedType.valueOf(rs.getString("bed_type")),
                        AvailabilityStatus.valueOf(rs.getString("availability_status"))
                );

                Reservation reservation = new Reservation(
                        rs.getString("reservation_id"),
                        customer,
                        room,
                        rs.getDate("booking_date").toLocalDate(),
                        rs.getDate("check_in_date").toLocalDate(),
                        rs.getDate("check_out_date").toLocalDate(),
                        rs.getInt("num_guests"),
                        rs.getDouble("booking_advance"),
                        ReservationStatus.valueOf(rs.getString("reservation_status"))
                );
                reservations.add(reservation);
            }
        }
        return reservations;
    }
}
