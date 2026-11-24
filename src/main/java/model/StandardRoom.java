package model;

/**
 * Concrete Room subclass for the Standard Room package.
 * Rate: LKR 6,000 | Breakfast: No | Extra Guest: LKR 1,000/guest/night | Tax: 10% | Base Capacity: 2 (Max: 3).
 */
public class StandardRoom extends Room {

    public StandardRoom(String roomId, BedType bedType, AvailabilityStatus availabilityStatus) {
        super(roomId, bedType, availabilityStatus);
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.STANDARD;
    }

    @Override
    public double getDailyRate() {
        return RoomType.STANDARD.getDailyRate();
    }

    @Override
    public boolean hasComplimentaryBreakfast() {
        return RoomType.STANDARD.hasComplimentaryBreakfast();
    }

    @Override
    public double getExtraGuestChargeRate() {
        return RoomType.STANDARD.getExtraGuestCharge();
    }

    @Override
    public double getTaxRate() {
        return RoomType.STANDARD.getTaxRate();
    }

    @Override
    public int getBaseCapacity() {
        return RoomType.STANDARD.getBaseCapacity();
    }

    @Override
    public int getMaxCapacity() {
        return RoomType.STANDARD.getMaxCapacity();
    }
}
