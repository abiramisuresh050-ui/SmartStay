package model;

/**
 * Concrete Room subclass for the Family Suite package.
 * Rate: LKR 18,000 | Breakfast: Yes | Extra Guest: LKR 2,500/guest/night | Tax: 15% | Base Capacity: 4 (Max: 6).
 */
public class FamilySuiteRoom extends Room {

    public FamilySuiteRoom(String roomId, BedType bedType, AvailabilityStatus availabilityStatus) {
        super(roomId, bedType, availabilityStatus);
    }

    @Override
    public RoomType getRoomType() {
        return RoomType.FAMILY_SUITE;
    }

    @Override
    public double getDailyRate() {
        return RoomType.FAMILY_SUITE.getDailyRate();
    }

    @Override
    public boolean hasComplimentaryBreakfast() {
        return RoomType.FAMILY_SUITE.hasComplimentaryBreakfast();
    }

    @Override
    public double getExtraGuestChargeRate() {
        return RoomType.FAMILY_SUITE.getExtraGuestCharge();
    }

    @Override
    public double getTaxRate() {
        return RoomType.FAMILY_SUITE.getTaxRate();
    }

    @Override
    public int getBaseCapacity() {
        return RoomType.FAMILY_SUITE.getBaseCapacity();
    }

    @Override
    public int getMaxCapacity() {
        return RoomType.FAMILY_SUITE.getMaxCapacity();
    }
}
