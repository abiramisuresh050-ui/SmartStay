package service;

import dao.RoomDAO;
import model.AvailabilityStatus;
import model.BedType;
import model.Room;
import model.RoomFactory;
import model.RoomType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Business logic service for Administrator Room Management.
 * Demonstrates explicit in-memory Data Structures ({@link ArrayList} and {@link HashMap})
 * and explicit Java Algorithms (Merge Sort and Linear Search) alongside JDBC persistence.
 */
public class RoomService {

    private final RoomDAO roomDAO;
    private final List<Room> roomListCache;
    private final Map<String, Room> roomMapCache;

    public RoomService() {
        this(new RoomDAO());
    }

    public RoomService(RoomDAO roomDAO) {
        this.roomDAO = roomDAO;
        this.roomListCache = new ArrayList<>();
        this.roomMapCache = new HashMap<>();
    }

    /**
     * Synchronizes in-memory ArrayList and HashMap caches with MySQL.
     */
    public List<Room> loadAllRooms() throws SQLException {
        roomListCache.clear();
        roomMapCache.clear();
        if (roomDAO != null) {
            List<Room> dbRooms = roomDAO.getAllRooms();
            for (Room room : dbRooms) {
                roomListCache.add(room);
                roomMapCache.put(room.getRoomId(), room);
            }
        }
        return mergeSortByRoomId(new ArrayList<>(roomListCache));
    }

    /**
     * Adds a new room using {@link RoomFactory} to instantiate the appropriate polymorphic subclass.
     */
    public Room addRoom(String roomId, RoomType roomType, BedType bedType, AvailabilityStatus status) throws SQLException {
        String normalizedId = roomId == null ? "" : roomId.trim().toUpperCase();
        if (normalizedId.isEmpty()) {
            throw new IllegalArgumentException("Room ID is required.");
        }
        if (roomMapCache.containsKey(normalizedId)) {
            throw new IllegalArgumentException("A room with ID " + normalizedId + " already exists.");
        }

        Room room = RoomFactory.createRoom(normalizedId, roomType, bedType, status);
        if (roomDAO != null) {
            roomDAO.addRoom(room);
        }
        roomListCache.add(room);
        roomMapCache.put(room.getRoomId(), room);
        return room;
    }

    /**
     * Updates an existing room record.
     */
    public Room updateRoom(String roomId, RoomType roomType, BedType bedType, AvailabilityStatus status) throws SQLException {
        String normalizedId = roomId == null ? "" : roomId.trim().toUpperCase();
        Room updated = RoomFactory.createRoom(normalizedId, roomType, bedType, status);
        if (roomDAO != null) {
            boolean ok = roomDAO.updateRoom(updated);
            if (!ok) {
                throw new IllegalArgumentException("Room " + normalizedId + " not found in database.");
            }
        }
        roomMapCache.put(normalizedId, updated);
        for (int i = 0; i < roomListCache.size(); i++) {
            if (roomListCache.get(i).getRoomId().equals(normalizedId)) {
                roomListCache.set(i, updated);
                break;
            }
        }
        return updated;
    }

    /**
     * Updates only a room's operational availability status.
     */
    public void updateRoomStatus(String roomId, AvailabilityStatus newStatus) throws SQLException {
        String normalizedId = roomId == null ? "" : roomId.trim().toUpperCase();
        if (roomDAO != null) {
            boolean ok = roomDAO.updateRoomStatus(normalizedId, newStatus);
            if (!ok) {
                throw new IllegalArgumentException("Room " + normalizedId + " not found.");
            }
        }
        Room cached = roomMapCache.get(normalizedId);
        if (cached != null) {
            cached.setAvailabilityStatus(newStatus);
        }
    }

    /**
     * Removes a room record from persistence and caches.
     */
    public void deleteRoom(String roomId) throws SQLException {
        String normalizedId = roomId == null ? "" : roomId.trim().toUpperCase();
        if (roomDAO != null) {
            boolean ok = roomDAO.deleteRoom(normalizedId);
            if (!ok) {
                throw new IllegalArgumentException("Room " + normalizedId + " could not be deleted.");
            }
        }
        Room removed = roomMapCache.remove(normalizedId);
        if (removed != null) {
            roomListCache.remove(removed);
        }
    }

    /**
     * O(1) average-time lookup using the in-memory {@link HashMap}.
     */
    public Room findRoomByIdFromCache(String roomId) {
        if (roomId == null) {
            return null;
        }
        return roomMapCache.get(roomId.trim().toUpperCase());
    }

    /**
     * Explicit Linear Search algorithm — O(n) — filtering rooms by type and AVAILABLE status.
     *
     * @param rooms source list of rooms
     * @param roomType target room type (or null to match all types)
     * @return list of available matching rooms
     */
    public List<Room> filterAvailableRoomsByType(List<Room> rooms, RoomType roomType) {
        List<Room> result = new ArrayList<>();
        for (Room room : rooms) {
            boolean statusMatches = room.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE;
            boolean typeMatches = (roomType == null) || (room.getRoomType() == roomType);
            if (statusMatches && typeMatches) {
                result.add(room);
            }
        }
        return result;
    }

    /**
     * Explicit recursive Merge Sort algorithm — O(n log n) — sorting rooms by Room ID ascending.
     */
    public List<Room> mergeSortByRoomId(List<Room> rooms) {
        if (rooms == null || rooms.size() <= 1) {
            return rooms;
        }
        int mid = rooms.size() / 2;
        List<Room> left = mergeSortByRoomId(new ArrayList<>(rooms.subList(0, mid)));
        List<Room> right = mergeSortByRoomId(new ArrayList<>(rooms.subList(mid, rooms.size())));
        return mergeByRoomId(left, right);
    }

    private List<Room> mergeByRoomId(List<Room> left, List<Room> right) {
        List<Room> merged = new ArrayList<>(left.size() + right.size());
        int i = 0;
        int j = 0;
        while (i < left.size() && j < right.size()) {
            if (left.get(i).getRoomId().compareToIgnoreCase(right.get(j).getRoomId()) <= 0) {
                merged.add(left.get(i++));
            } else {
                merged.add(right.get(j++));
            }
        }
        while (i < left.size()) {
            merged.add(left.get(i++));
        }
        while (j < right.size()) {
            merged.add(right.get(j++));
        }
        return merged;
    }

    /**
     * Returns an unmodifiable copy of the current in-memory room list (useful for unit tests).
     */
    public List<Room> getCachedRooms() {
        return new ArrayList<>(roomListCache);
    }
}
