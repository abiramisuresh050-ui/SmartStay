package model;

import java.util.Objects;

/**
 * Represents a registered hotel guest (local or international).
 * Demonstrates encapsulation with validation of mandatory registration attributes.
 */
public class Customer {

    private int customerId;
    private String nicPassport;
    private String fullName;
    private String contactNumber;
    private String email;

    public Customer(int customerId, String nicPassport, String fullName, String contactNumber, String email) {
        this.customerId = customerId;
        setNicPassport(nicPassport);
        setFullName(fullName);
        setContactNumber(contactNumber);
        setEmail(email);
    }

    public Customer(String nicPassport, String fullName, String contactNumber, String email) {
        this(0, nicPassport, fullName, contactNumber, email);
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getNicPassport() {
        return nicPassport;
    }

    public void setNicPassport(String nicPassport) {
        if (nicPassport == null || nicPassport.trim().isEmpty()) {
            throw new IllegalArgumentException("NIC/Passport number is required.");
        }
        this.nicPassport = nicPassport.trim();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        this.fullName = fullName.trim();
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        if (contactNumber == null || contactNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Contact number is required.");
        }
        this.contactNumber = contactNumber.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            throw new IllegalArgumentException("A valid email address is required.");
        }
        this.email = email.trim();
    }

    @Override
    public String toString() {
        return fullName + " (" + nicPassport + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Customer)) {
            return false;
        }
        Customer other = (Customer) obj;
        return Objects.equals(this.nicPassport, other.nicPassport);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nicPassport);
    }
}
