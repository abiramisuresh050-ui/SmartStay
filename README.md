# SmartStay Hotel Booking System

**Module:** CI6115 Programming III – Patterns and Algorithms (Kingston University / ESoft)  
**Stack:** Java 17 (OpenJDK) | Java Swing | Maven | MySQL Server & Connector/J (JDBC) | JUnit 5

---

## Project Architecture

```text
SmartStay
├── pom.xml
├── sql/
│   └── smartstay_schema.sql
├── src/main/java/
│   ├── model/
│   │   ├── RoomType.java, BedType.java, AvailabilityStatus.java, ReservationStatus.java
│   │   ├── Room.java (abstract), StandardRoom.java, DeluxeRoom.java, SuiteRoom.java, FamilySuiteRoom.java
│   │   ├── RoomFactory.java
│   │   ├── Customer.java
│   │   ├── Reservation.java
│   │   ├── Invoice.java
│   │   └── Payment.java
│   ├── database/
│   │   └── DatabaseConnection.java (Singleton JDBC)
│   ├── dao/
│   │   ├── CustomerDAO.java
│   │   ├── RoomDAO.java
│   │   └── ReservationDAO.java
│   ├── service/
│   │   ├── RoomService.java
│   │   ├── BookingService.java
│   │   └── InvoiceService.java
│   └── view/
│       ├── MainFrame.java
│       ├── RoomForm.java
│       ├── GuestForm.java
│       ├── BookingForm.java
│       └── InvoiceForm.java
└── src/test/java/service/
    ├── InvoiceServiceTest.java
    ├── BookingServiceTest.java
    └── RoomServiceTest.java
```

---

## Documented Coursework Assumptions

1. **Room Base & Maximum Capacity (`[ASSUMPTION-01]`):** Because the specification requires extra guest charges beyond room capacity without defining capacities, base and maximum capacities are defined by Room Type:
   - `Standard`: Base = **2** guests (Max = 3)
   - `Deluxe`: Base = **2** guests (Max = 4)
   - `Suite`: Base = **3** guests (Max = 5)
   - `Family Suite`: Base = **4** guests (Max = 6)
2. **Extra Guest Charge (`[ASSUMPTION-02]`):** Charged per extra guest **per night**: `extraGuests * extraGuestRate * numberOfNights`.
3. **Discount & Tax Base (`[ASSUMPTION-03]`):** The 12% long-stay discount ($\ge 5$ nights) applies strictly to the Base Room Charge; room-type tax (10%, 12%, 15%) applies to `(Base Room Charge - Discount) + Extra Guest Charge`.
4. **Refundable Booking Advance (`[ASSUMPTION-04]`):** If the gross bill is less than LKR 10,000 (e.g., 1 night in a Standard room = LKR 6,600), the invoice displays `Final Payable Amount: LKR 0.00` and `Refund Due to Guest: LKR 3,400.00`.
5. **24-Hour Modification/Cancellation Freeze (`[ASSUMPTION-06]`):** Evaluated against standard hotel check-in time `14:00` on the `checkInDate`.

> **Coursework Reminder:** Prefix every class name in your **UML Class Diagram** with your Kingston University Student ID (e.g., `K24xxxxx_Room`) before submitting your report.

---

## Setup & Running on Ubuntu 24.04 LTS / Apache NetBeans

### 1. Initialize MySQL Database
```bash
sudo mysql < sql/smartstay_schema.sql
```

### 2. Run JUnit 5 Unit Tests
```bash
mvn test
```

### 3. Run the Java Swing Application
Set optional MySQL credentials if your local MySQL root account uses a password:
```bash
export SMARTSTAY_DB_USER="root"
export SMARTSTAY_DB_PASSWORD="your_password"
mvn exec:java
```
Or open this directory directly as a **Maven Project** in **Apache NetBeans** and run `view.MainFrame`.
