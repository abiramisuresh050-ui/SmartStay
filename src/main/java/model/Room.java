package model;

import java.util.Objects;

/**
 * Abstract base class representing a hotel room in the SmartStay system.
 * Demonstrates OOP Abstraction, Encapsulation, Inheritance, and Polymorphism.
 * Concrete subclasses ({@link StandardRoom}, {@link DeluxeRoom}, {@link SuiteRoom},
 * {@link FamilySuiteRoom}) override package-specific pricing, capacity, breakfast,
 * and tax behaviours.
 */
public abstract class Room {

    private String roomId;
    private BedType bedType;
    private AvailabilityStatus availabilityStatus;

    protected Room(String roomId, BedType bedType, AvailabilityStatus availabilityStatus) {
        setRoomId(roomId);
        setBedType(bedType);
        setAvailabilityStatus(availabilityStatus);
    }

    /**
     * @return the specific RoomType implemented by this subclass.
     */
    public abstract RoomType getRoomType();

    /**
     * @return the daily room rate in LKR.
     */
    public abstract double getDailyRate();

    /**
     * @return true if complimentary breakfast is included with this room package.
     */
    public abstract boolean hasComplimentaryBreakfast();

    /**
     * @return the extra guest charge per additional guest per night in LKR.
     */
    public abstract double getExtraGuestChargeRate();

    /**
     * @return the applicable tax rate as a decimal (e.g., 0.10 for 10%).
     */
    public abstract double getTaxRate();

    /**
     * Documented Assumption [ASSUMPTION-01]:
     * Base guest capacity included in the daily room rate before extra guest fees apply.
     *
     * @return base guest capacity.
     */
    public abstract int getBaseCapacity();

    /**
     * Documented Assumption [ASSUMPTION-01]:
     * Maximum permitted total occupancy (base capacity + allowed extra guests).
     *
     * @return maximum total guest occupancy.
     */
    public abstract int getMaxCapacity();

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            throw new IllegalArgumentException("Room ID cannot be empty.");
        }
        this.roomId = roomId.trim().toUpperCase();
    }

    public BedType getBedType() {
        return bedType;
    }

    public void setBedType(BedType bedType) {
        if (bedType == null) {
            throw new IllegalArgumentException("Bed type cannot be null.");
        }
        this.bedType = bedType;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(AvailabilityStatus availabilityStatus) {
        if (availabilityStatus == null) {
            throw new IllegalArgumentException("Availability status cannot be null.");
        }
        this.availabilityStatus = availabilityStatus;
    }

    public boolean isAvailable() {
        return this.availabilityStatus == AvailabilityStatus.AVAILABLE;
    }

    @Override
    public String toString() {
        return roomId + " - " + getRoomType().getDisplayName() + " (" + bedType.getDisplayName() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Room)) {
            return false;
        }
        Room other = (Room) obj;
        return Objects.equals(this.roomId, other.roomId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId);
    }
}
