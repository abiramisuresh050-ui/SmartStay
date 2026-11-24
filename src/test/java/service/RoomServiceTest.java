package service;

import model.AvailabilityStatus;
import model.BedType;
import model.DeluxeRoom;
import model.FamilySuiteRoom;
import model.Room;
import model.RoomType;
import model.StandardRoom;
import model.SuiteRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JUnit 5 test suite validating Room CRUD logic, OOP polymorphism via RoomFactory,
 * HashMap cache O(1) lookup, and Merge Sort by Room ID.
 */
class RoomServiceTest {

    private RoomService roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService(null);
    }

    @Test
    @DisplayName("TC-RM-01: RoomFactory instantiates correct polymorphic subclasses and package rates")
    void testPolymorphicRoomSubclasses() throws SQLException {
        Room r1 = roomService.addRoom("R301", RoomType.SUITE, BedType.KING, AvailabilityStatus.AVAILABLE);
        Room r2 = roomService.addRoom("R101", RoomType.STANDARD, BedType.SINGLE, AvailabilityStatus.AVAILABLE);
        Room r3 = roomService.addRoom("R201", RoomType.DELUXE, BedType.DOUBLE, AvailabilityStatus.AVAILABLE);
        Room r4 = roomService.addRoom("R401", RoomType.FAMILY_SUITE, BedType.QUEEN, AvailabilityStatus.UNDER_MAINTENANCE);

        assertInstanceOf(SuiteRoom.class, r1);
        assertInstanceOf(StandardRoom.class, r2);
        assertInstanceOf(DeluxeRoom.class, r3);
        assertInstanceOf(FamilySuiteRoom.class, r4);

        assertEquals(14000.00, r1.getDailyRate(), 0.01);
        assertEquals(6000.00, r2.getDailyRate(), 0.01);
        assertEquals(9000.00, r3.getDailyRate(), 0.01);
        assertEquals(18000.00, r4.getDailyRate(), 0.01);

        // O(1) HashMap lookup
        assertNotNull(roomService.findRoomByIdFromCache("R201"));

        // Duplicate Room ID rejected
        assertThrows(IllegalArgumentException.class, () ->
                roomService.addRoom("R101", RoomType.STANDARD, BedType.DOUBLE, AvailabilityStatus.AVAILABLE));

        // Merge Sort by Room ID ascending
        List<Room> sorted = roomService.mergeSortByRoomId(roomService.getCachedRooms());
        assertEquals("R101", sorted.get(0).getRoomId());
        assertEquals("R201", sorted.get(1).getRoomId());
        assertEquals("R301", sorted.get(2).getRoomId());
        assertEquals("R401", sorted.get(3).getRoomId());
    }
}
