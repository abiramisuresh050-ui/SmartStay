package dao;

import database.DatabaseConnection;
import model.AvailabilityStatus;
import model.BedType;
import model.Room;
import model.RoomFactory;
import model.RoomType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for CRUD operations on {@link Room} records in MySQL.
 * Uses {@link RoomFactory} to instantiate the appropriate polymorphic Room subclass
 * from single-table relational rows.
 */
public class RoomDAO {

    /**
     * Inserts a new room record into the database.
     */
    public boolean addRoom(Room room) throws SQLException {
        String sql = "INSERT INTO rooms (room_id, room_type, bed_type, daily_rate, base_capacity, max_capacity, "
                + "complimentary_breakfast, extra_guest_charge, tax_rate, availability_status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, room.getRoomId());
            stmt.setString(2, room.getRoomType().name());
            stmt.setString(3, room.getBedType().name());
            stmt.setDouble(4, room.getDailyRate());
            stmt.setInt(5, room.getBaseCapacity());
            stmt.setInt(6, room.getMaxCapacity());
            stmt.setBoolean(7, room.hasComplimentaryBreakfast());
            stmt.setDouble(8, room.getExtraGuestChargeRate());
            stmt.setDouble(9, room.getTaxRate() * 100.0);
            stmt.setString(10, room.getAvailabilityStatus().name());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Updates an existing room record.
     */
    public boolean updateRoom(Room room) throws SQLException {
        String sql = "UPDATE rooms SET room_type = ?, bed_type = ?, daily_rate = ?, base_capacity = ?, "
                + "max_capacity = ?, complimentary_breakfast = ?, extra_guest_charge = ?, tax_rate = ?, "
                + "availability_status = ? WHERE room_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, room.getRoomType().name());
            stmt.setString(2, room.getBedType().name());
            stmt.setDouble(3, room.getDailyRate());
            stmt.setInt(4, room.getBaseCapacity());
            stmt.setInt(5, room.getMaxCapacity());
            stmt.setBoolean(6, room.hasComplimentaryBreakfast());
            stmt.setDouble(7, room.getExtraGuestChargeRate());
            stmt.setDouble(8, room.getTaxRate() * 100.0);
            stmt.setString(9, room.getAvailabilityStatus().name());
            stmt.setString(10, room.getRoomId());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Updates only the availability status of a room.
     */
    public boolean updateRoomStatus(String roomId, AvailabilityStatus status) throws SQLException {
        String sql = "UPDATE rooms SET availability_status = ? WHERE room_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setString(2, roomId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a room record by its unique Room ID.
     */
    public boolean deleteRoom(String roomId) throws SQLException {
        String sql = "DELETE FROM rooms WHERE room_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all room records from MySQL into an {@link ArrayList}.
     */
    public List<Room> getAllRooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT room_id, room_type, bed_type, availability_status FROM rooms";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rooms.add(mapRow(rs));
            }
        }
        return rooms;
    }

    /**
     * Retrieves a single room by its unique Room ID.
     */
    public Room getRoomById(String roomId) throws SQLException {
        String sql = "SELECT room_id, room_type, bed_type, availability_status FROM rooms WHERE room_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roomId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        String roomId = rs.getString("room_id");
        RoomType roomType = RoomType.valueOf(rs.getString("room_type"));
        BedType bedType = BedType.valueOf(rs.getString("bed_type"));
        AvailabilityStatus status = AvailabilityStatus.valueOf(rs.getString("availability_status"));
        return RoomFactory.createRoom(roomId, roomType, bedType, status);
    }
}
