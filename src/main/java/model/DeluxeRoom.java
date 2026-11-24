package model;

/**
 * Concrete Room subclass for the Deluxe Room package.
 * Rate: LKR 9,000 | Breakfast: Yes | Extra Guest: LKR 1,500/guest/night | Tax: 12% | Base Capacity: 2 (Max: 4).
 */
public class DeluxeRoom extends Room {

    public DeluxeRoom(String roomId, BedType bedType, AvailabilityStatus availabilityStatus) {
        super(roomId, bedType, availabilityStatus);
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.DELUXE;
    }

    @Override
    public double getDailyRate() {
        return RoomType.DELUXE.getDailyRate();
    }

    @Override
    public boolean hasComplimentaryBreakfast() {
        return RoomType.DELUXE.hasComplimentaryBreakfast();
    }

    @Override
    public double getExtraGuestChargeRate() {
        return RoomType.DELUXE.getExtraGuestCharge();
    }

    @Override
    public double getTaxRate() {
        return RoomType.DELUXE.getTaxRate();
    }

    @Override
    public int getBaseCapacity() {
        return RoomType.DELUXE.getBaseCapacity();
    }

    @Override
    public int getMaxCapacity() {
        return RoomType.DELUXE.getMaxCapacity();
    }
}
