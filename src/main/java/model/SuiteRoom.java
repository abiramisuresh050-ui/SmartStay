package model;

/**
 * Concrete Room subclass for the Suite Room package.
 * Rate: LKR 14,000 | Breakfast: Yes | Extra Guest: LKR 2,000/guest/night | Tax: 15% | Base Capacity: 3 (Max: 5).
 */
public class SuiteRoom extends Room {

    public SuiteRoom(String roomId, BedType bedType, AvailabilityStatus availabilityStatus) {
        super(roomId, bedType, availabilityStatus);
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.SUITE;
    }

    @Override
    public double getDailyRate() {
        return RoomType.SUITE.getDailyRate();
    }

    @Override
    public boolean hasComplimentaryBreakfast() {
        return RoomType.SUITE.hasComplimentaryBreakfast();
    }

    @Override
    public double getExtraGuestChargeRate() {
        return RoomType.SUITE.getExtraGuestCharge();
    }

    @Override
    public double getTaxRate() {
        return RoomType.SUITE.getTaxRate();
    }

    @Override
    public int getBaseCapacity() {
        return RoomType.SUITE.getBaseCapacity();
    }

    @Override
    public int getMaxCapacity() {
        return RoomType.SUITE.getMaxCapacity();
    }
}
