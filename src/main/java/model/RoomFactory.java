package model;

/**
 * Factory Pattern implementation for instantiating concrete {@link Room} subclasses
 * based on the specified {@link RoomType}.
 */
public final class RoomFactory {

    private RoomFactory() {
        // Utility factory class
    }

    /**
     * Creates the appropriate polymorphic {@link Room} subclass for the given RoomType.
     *
     * @param roomId unique room identifier
     * @param roomType package category
     * @param bedType bed configuration
     * @param status operational availability status
     * @return concrete Room instance
     */
    public static Room createRoom(String roomId, RoomType roomType, BedType bedType, AvailabilityStatus status) {
        if (roomType == null) {
            throw new IllegalArgumentException("Room type must not be null.");
        }
        return switch (roomType) {
            case STANDARD -> new StandardRoom(roomId, bedType, status);
            case DELUXE -> new DeluxeRoom(roomId, bedType, status);
            case SUITE -> new SuiteRoom(roomId, bedType, status);
            case FAMILY_SUITE -> new FamilySuiteRoom(roomId, bedType, status);
        };
    }
}
