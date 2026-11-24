package service;

import dao.CustomerDAO;
import dao.ReservationDAO;
import dao.RoomDAO;
import model.AvailabilityStatus;
import model.Customer;
import model.Reservation;
import model.ReservationStatus;
import model.Room;
import model.RoomType;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core business logic service for Customer Registration, Room Availability Search,
 * Reservation Creation, Modification, Cancellation, Sorting, and Searching.
 *
 * <p>Explicitly implements Data Structures ({@link ArrayList}, {@link HashMap}) and
 * Algorithms (Merge Sort O(n log n), Binary Search O(log n), Linear Search O(n),
 * and Date-Interval Overlap Detection O(n)) in Java.</p>
 */
public class BookingService {

    /** Minimum advance booking notice required (2 days before check-in). */
    public static final long MIN_ADVANCE_BOOKING_DAYS = 2;

    /** Minimum notice required before check-in to modify or cancel a reservation (24 hours). */
    public static final long CANCELLATION_FREEZE_HOURS = 24;

    /** Standard hotel check-in time (14:00) used to evaluate the 24-hour freeze rule [ASSUMPTION-06]. */
    public static final LocalTime STANDARD_CHECK_IN_TIME = LocalTime.of(14, 0);

    private final CustomerDAO customerDAO;
    private final RoomDAO roomDAO;
    private final ReservationDAO reservationDAO;

    private final List<Customer> customerCache;
    private final Map<String, Customer> customerByNicMap;
    private final List<Reservation> reservationCache;

    public BookingService() {
        this(new CustomerDAO(), new RoomDAO(), new ReservationDAO());
    }

    public BookingService(CustomerDAO customerDAO, RoomDAO roomDAO, ReservationDAO reservationDAO) {
        this.customerDAO = customerDAO;
        this.roomDAO = roomDAO;
        this.reservationDAO = reservationDAO;
        this.customerCache = new ArrayList<>();
        this.customerByNicMap = new HashMap<>();
        this.reservationCache = new ArrayList<>();
    }

    // =========================================================================
    // Customer Registration & Retrieval
    // =========================================================================

    public Customer registerCustomer(String nicPassport, String fullName, String contactNumber, String email)
            throws SQLException {
        Customer customer = new Customer(nicPassport, fullName, contactNumber, email);
        if (customerDAO != null) {
            Customer existing = customerDAO.getCustomerByNicPassport(customer.getNicPassport());
            if (existing != null) {
                throw new IllegalArgumentException("A guest with NIC/Passport '" + customer.getNicPassport() + "' is already registered.");
            }
            customer = customerDAO.addCustomer(customer);
        } else {
            if (customerByNicMap.containsKey(customer.getNicPassport())) {
                throw new IllegalArgumentException("A guest with NIC/Passport '" + customer.getNicPassport() + "' is already registered.");
            }
            customer.setCustomerId(customerCache.size() + 1);
        }
        customerCache.add(customer);
        customerByNicMap.put(customer.getNicPassport(), customer);
        return customer;
    }

    public boolean updateCustomer(Customer customer) throws SQLException {
        boolean updated = (customerDAO == null) || customerDAO.updateCustomer(customer);
        if (updated) {
            customerByNicMap.put(customer.getNicPassport(), customer);
        }
        return updated;
    }

    public List<Customer> loadAllCustomers() throws SQLException {
        customerCache.clear();
        customerByNicMap.clear();
        if (customerDAO != null) {
            for (Customer c : customerDAO.getAllCustomers()) {
                customerCache.add(c);
                customerByNicMap.put(c.getNicPassport(), c);
            }
        }
        return new ArrayList<>(customerCache);
    }

    /**
     * O(1) HashMap lookup of a customer by NIC/Passport.
     */
    public Customer findCustomerByNicFromCache(String nicPassport) {
        return customerByNicMap.get(nicPassport == null ? "" : nicPassport.trim());
    }

    // =========================================================================
    // Room Availability Search & Date-Interval Overlap Detection Algorithm
    // =========================================================================

    /**
     * Checks whether two date intervals [start1, end1) and [start2, end2) overlap.
     * Overlap exists iff start1 < end2 AND end1 > start2.
     */
    public boolean hasDateOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    /**
     * Searches available rooms matching the requested {@link RoomType} and date range.
     * A room is eligible iff:
     * 1. Its availability status is AVAILABLE (not RESERVED or UNDER_MAINTENANCE).
     * 2. Its RoomType matches the requested type (if specified).
     * 3. No active CONFIRMED reservation overlaps [checkInDate, checkOutDate).
     */
    public List<Room> searchAvailableRooms(List<Room> allRooms, List<Reservation> allReservations,
                                           RoomType roomType, LocalDate checkInDate, LocalDate checkOutDate) {
        validateDateRange(checkInDate, checkOutDate);
        List<Room> availableRooms = new ArrayList<>();

        for (Room room : allRooms) {
            if (room.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
                continue;
            }
            if (roomType != null && room.getRoomType() != roomType) {
                continue;
            }

            boolean conflictFound = false;
            for (Reservation res : allReservations) {
                if (res.getStatus() == ReservationStatus.CONFIRMED
                        && res.getRoom().getRoomId().equals(room.getRoomId())
                        && hasDateOverlap(checkInDate, checkOutDate, res.getCheckInDate(), res.getCheckOutDate())) {
                    conflictFound = true;
                    break;
                }
            }

            if (!conflictFound) {
                availableRooms.add(room);
            }
        }
        return availableRooms;
    }

    /**
     * Database-backed convenience overload for the Swing UI.
     */
    public List<Room> searchAvailableRooms(RoomType roomType, LocalDate checkInDate, LocalDate checkOutDate)
            throws SQLException {
        List<Room> allRooms = (roomDAO != null) ? roomDAO.getAllRooms() : new ArrayList<>();
        List<Reservation> allReservations = loadAllReservations();
        return searchAvailableRooms(allRooms, allReservations, roomType, checkInDate, checkOutDate);
    }

    // =========================================================================
    // Reservation Creation, Modification & Cancellation (Business Rules)
    // =========================================================================

    /**
     * Validates business rules and creates a new reservation with the mandatory
     * LKR 10,000 refundable booking advance.
     */
    public Reservation createReservation(Customer customer, Room room, int numberOfGuests,
                                         LocalDate bookingDate, LocalDate checkInDate, LocalDate checkOutDate)
            throws SQLException {
        if (customer == null) {
            throw new IllegalArgumentException("A registered customer must be selected.");
        }
        if (room == null) {
            throw new IllegalArgumentException("A room must be selected.");
        }
        if (room.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
            throw new IllegalStateException("Room " + room.getRoomId() + " is not available (Status: "
                    + room.getAvailabilityStatus().getDisplayName() + ").");
        }

        validateDateRange(checkInDate, checkOutDate);
        validateAdvanceBookingRule(bookingDate, checkInDate);
        validateGuestCount(room, numberOfGuests);

        // Verify no overlapping confirmed reservations for the same room
        List<Reservation> existingList = (reservationDAO != null) ? reservationDAO.getAllReservations() : reservationCache;
        for (Reservation existing : existingList) {
            if (existing.getStatus() == ReservationStatus.CONFIRMED
                    && existing.getRoom().getRoomId().equals(room.getRoomId())
                    && hasDateOverlap(checkInDate, checkOutDate, existing.getCheckInDate(), existing.getCheckOutDate())) {
                throw new IllegalStateException("Room " + room.getRoomId() + " already has a confirmed booking overlapping these dates.");
            }
        }

        String reservationId = generateNextReservationId(existingList);
        Reservation reservation = new Reservation(
                reservationId,
                customer,
                room,
                bookingDate,
                checkInDate,
                checkOutDate,
                numberOfGuests,
                Reservation.STANDARD_BOOKING_ADVANCE,
                ReservationStatus.CONFIRMED
        );

        if (reservationDAO != null) {
            reservationDAO.createReservation(reservation);
        }
        room.setAvailabilityStatus(AvailabilityStatus.RESERVED);
        reservationCache.add(reservation);
        return reservation;
    }

    /**
     * Enforces Business Rule BR-09: Guests must book at least 2 days in advance.
     */
    public void validateAdvanceBookingRule(LocalDate bookingDate, LocalDate checkInDate) {
        long daysInAdvance = ChronoUnit.DAYS.between(bookingDate, checkInDate);
        if (daysInAdvance < MIN_ADVANCE_BOOKING_DAYS) {
            throw new IllegalArgumentException(
                    "Guests must book at least " + MIN_ADVANCE_BOOKING_DAYS + " days in advance of the check-in date.");
        }
    }

    /**
     * Enforces Business Rule BR-08: Check-out date must be strictly after check-in date.
     */
    public void validateDateRange(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException("Both check-in and check-out dates are required.");
        }
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("Check-out date must be at least 1 day after the check-in date.");
        }
    }

    /**
     * Validates guest count against minimum (1) and the room's documented maximum capacity.
     */
    public void validateGuestCount(Room room, int numberOfGuests) {
        if (numberOfGuests < 1) {
            throw new IllegalArgumentException("Number of guests must be at least 1.");
        }
        if (numberOfGuests > room.getMaxCapacity()) {
            throw new IllegalArgumentException(
                    "Maximum occupancy for " + room.getRoomType().getDisplayName()
                            + " is " + room.getMaxCapacity() + " guests (Base capacity: " + room.getBaseCapacity() + ").");
        }
    }

    /**
     * Enforces Business Rule BR-10: Reservations cannot be modified or cancelled
     * within 24 hours of the check-in date (evaluated against 14:00 check-in time).
     */
    public boolean isEligibleForModificationOrCancellation(Reservation reservation, LocalDateTime currentDateTime) {
        if (reservation == null || reservation.getStatus() != ReservationStatus.CONFIRMED) {
            return false;
        }
        LocalDateTime checkInDateTime = reservation.getCheckInDate().atTime(STANDARD_CHECK_IN_TIME);
        long hoursRemaining = Duration.between(currentDateTime, checkInDateTime).toHours();
        return hoursRemaining >= CANCELLATION_FREEZE_HOURS;
    }

    /**
     * Modifies an eligible confirmed reservation (dates and guest count).
     */
    public void modifyReservation(Reservation reservation, LocalDate newCheckIn, LocalDate newCheckOut,
                                  int newGuests, LocalDateTime currentDateTime) throws SQLException {
        if (!isEligibleForModificationOrCancellation(reservation, currentDateTime)) {
            throw new IllegalStateException(
                    "Reservations cannot be modified within 24 hours of check-in or after cancellation/completion.");
        }
        validateDateRange(newCheckIn, newCheckOut);
        validateAdvanceBookingRule(currentDateTime.toLocalDate(), newCheckIn);
        validateGuestCount(reservation.getRoom(), newGuests);

        reservation.setCheckInDate(newCheckIn);
        reservation.setCheckOutDate(newCheckOut);
        reservation.setNumberOfGuests(newGuests);

        if (reservationDAO != null) {
            reservationDAO.updateReservation(reservation);
        }
    }

    /**
     * Cancels an eligible confirmed reservation, marking the LKR 10,000 advance as refunded
     * and returning the room to AVAILABLE status.
     */
    public void cancelReservation(Reservation reservation, LocalDateTime currentDateTime) throws SQLException {
        if (!isEligibleForModificationOrCancellation(reservation, currentDateTime)) {
            throw new IllegalStateException(
                    "Reservations cannot be cancelled within 24 hours of the check-in date (or are already inactive).");
        }

        if (reservationDAO != null) {
            reservationDAO.cancelReservation(reservation);
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.getRoom().setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
    }

    public List<Reservation> loadAllReservations() throws SQLException {
        reservationCache.clear();
        if (reservationDAO != null) {
            reservationCache.addAll(reservationDAO.getAllReservations());
        }
        return new ArrayList<>(reservationCache);
    }

    // =========================================================================
    // Explicit Sorting & Searching Algorithms (CI6115 Core Assessment)
    // =========================================================================

    /**
     * Explicit recursive Merge Sort algorithm — O(n log n) — sorting reservations
     * chronologically by Check-in Date ascending.
     */
    public List<Reservation> mergeSortByCheckInDate(List<Reservation> reservations) {
        if (reservations == null || reservations.size() <= 1) {
            return reservations;
        }
        int mid = reservations.size() / 2;
        List<Reservation> left = mergeSortByCheckInDate(new ArrayList<>(reservations.subList(0, mid)));
        List<Reservation> right = mergeSortByCheckInDate(new ArrayList<>(reservations.subList(mid, reservations.size())));
        return mergeByCheckInDate(left, right);
    }

    private List<Reservation> mergeByCheckInDate(List<Reservation> left, List<Reservation> right) {
        List<Reservation> merged = new ArrayList<>(left.size() + right.size());
        int i = 0;
        int j = 0;
        while (i < left.size() && j < right.size()) {
            if (!left.get(i).getCheckInDate().isAfter(right.get(j).getCheckInDate())) {
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
     * Explicit recursive Merge Sort algorithm — O(n log n) — sorting reservations
     * lexicographically by Booking ID ascending (pre-condition for Binary Search).
     */
    public List<Reservation> mergeSortByBookingId(List<Reservation> reservations) {
        if (reservations == null || reservations.size() <= 1) {
            return reservations;
        }
        int mid = reservations.size() / 2;
        List<Reservation> left = mergeSortByBookingId(new ArrayList<>(reservations.subList(0, mid)));
        List<Reservation> right = mergeSortByBookingId(new ArrayList<>(reservations.subList(mid, reservations.size())));
        return mergeByBookingId(left, right);
    }

    private List<Reservation> mergeByBookingId(List<Reservation> left, List<Reservation> right) {
        List<Reservation> merged = new ArrayList<>(left.size() + right.size());
        int i = 0;
        int j = 0;
        while (i < left.size() && j < right.size()) {
            if (left.get(i).getReservationId().compareToIgnoreCase(right.get(j).getReservationId()) <= 0) {
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
     * Explicit iterative Binary Search algorithm — O(log n) — locating a reservation
     * by exact Booking ID. Automatically sorts a copy by Booking ID first to guarantee correctness.
     *
     * @param reservations list of reservations
     * @param targetBookingId exact booking ID to search (e.g., "BK001")
     * @return matching Reservation, or null if not found
     */
    public Reservation binarySearchByBookingId(List<Reservation> reservations, String targetBookingId) {
        if (reservations == null || reservations.isEmpty() || targetBookingId == null || targetBookingId.trim().isEmpty()) {
            return null;
        }
        List<Reservation> sorted = mergeSortByBookingId(new ArrayList<>(reservations));
        String target = targetBookingId.trim();

        int low = 0;
        int high = sorted.size() - 1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            String midId = sorted.get(mid).getReservationId();
            int cmp = midId.compareToIgnoreCase(target);
            if (cmp == 0) {
                return sorted.get(mid);
            } else if (cmp < 0) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return null;
    }

    /**
     * Explicit Linear Search algorithm — O(n) — searching reservations by guest full name
     * (case-insensitive substring match).
     */
    public List<Reservation> linearSearchByGuestName(List<Reservation> reservations, String guestNameQuery) {
        List<Reservation> matches = new ArrayList<>();
        if (reservations == null || guestNameQuery == null || guestNameQuery.trim().isEmpty()) {
            return matches;
        }
        String query = guestNameQuery.trim().toLowerCase();
        for (Reservation res : reservations) {
            if (res.getCustomer() != null
                    && res.getCustomer().getFullName().toLowerCase().contains(query)) {
                matches.add(res);
            }
        }
        return matches;
    }

    /**
     * Explicit Linear Search algorithm — O(n) — filtering reservations by a specific check-in date.
     */
    public List<Reservation> filterReservationsByCheckInDate(List<Reservation> reservations, LocalDate checkInDate) {
        List<Reservation> matches = new ArrayList<>();
        if (reservations == null || checkInDate == null) {
            return matches;
        }
        for (Reservation res : reservations) {
            if (checkInDate.equals(res.getCheckInDate())) {
                matches.add(res);
            }
        }
        return matches;
    }

    private String generateNextReservationId(List<Reservation> existing) {
        int maxNum = 0;
        for (Reservation res : existing) {
            String id = res.getReservationId();
            if (id != null && id.startsWith("BK")) {
                try {
                    int num = Integer.parseInt(id.substring(2));
                    if (num > maxNum) {
                        maxNum = num;
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore non-standard IDs
                }
            }
        }
        return String.format("BK%03d", maxNum + 1);
    }

    public List<Reservation> getCachedReservations() {
        return new ArrayList<>(reservationCache);
    }
}
