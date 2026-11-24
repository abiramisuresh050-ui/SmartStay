package model;

/**
 * Enum representing the four SmartStay room package categories defined in Table 1.
 * Encapsulates standard package metadata including daily rate, complimentary breakfast,
 * extra guest charge, tax rate, and documented base/maximum guest capacities.
 */
public enum RoomType {
    STANDARD("Standard", 6000.00, false, 1000.00, 0.10, 2, 3),
    DELUXE("Deluxe", 9000.00, true, 1500.00, 0.12, 2, 4),
    SUITE("Suite", 14000.00, true, 2000.00, 0.15, 3, 5),
    FAMILY_SUITE("Family Suite", 18000.00, true, 2500.00, 0.15, 4, 6);

    private final String displayName;
    private final double dailyRate;
    private final boolean complimentaryBreakfast;
    private final double extraGuestCharge;
    private final double taxRate;
    private final int baseCapacity;
    private final int maxCapacity;

    RoomType(String displayName, double dailyRate, boolean complimentaryBreakfast,
             double extraGuestCharge, double taxRate, int baseCapacity, int maxCapacity) {
        this.displayName = displayName;
        this.dailyRate = dailyRate;
        this.complimentaryBreakfast = complimentaryBreakfast;
        this.extraGuestCharge = extraGuestCharge;
        this.taxRate = taxRate;
        this.baseCapacity = baseCapacity;
        this.maxCapacity = maxCapacity;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getDailyRate() {
        return dailyRate;
    }

    public boolean hasComplimentaryBreakfast() {
        return complimentaryBreakfast;
    }

    public double getExtraGuestCharge() {
        return extraGuestCharge;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public int getBaseCapacity() {
        return baseCapacity;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
