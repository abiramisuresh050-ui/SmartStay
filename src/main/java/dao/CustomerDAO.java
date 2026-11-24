package dao;

import database.DatabaseConnection;
import model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for persisting and retrieving {@link Customer} records via JDBC.
 */
public class CustomerDAO {

    /**
     * Inserts a new customer into MySQL and populates the generated primary key.
     *
     * @param customer the customer to register
     * @return the saved Customer with its generated customerId
     * @throws SQLException if a database error or duplicate NIC/Passport occurs
     */
    public Customer addCustomer(Customer customer) throws SQLException {
        String sql = "INSERT INTO customers (nic_passport, full_name, contact_number, email) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, customer.getNicPassport());
            stmt.setString(2, customer.getFullName());
            stmt.setString(3, customer.getContactNumber());
            stmt.setString(4, customer.getEmail());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    customer.setCustomerId(keys.getInt(1));
                }
            }
            return customer;
        }
    }

    /**
     * Updates an existing customer's contact details.
     *
     * @param customer updated customer record
     * @return true if a row was updated
     * @throws SQLException if a database error occurs
     */
    public boolean updateCustomer(Customer customer) throws SQLException {
        String sql = "UPDATE customers SET nic_passport = ?, full_name = ?, contact_number = ?, email = ? WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customer.getNicPassport());
            stmt.setString(2, customer.getFullName());
            stmt.setString(3, customer.getContactNumber());
            stmt.setString(4, customer.getEmail());
            stmt.setInt(5, customer.getCustomerId());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all registered customers into an {@link ArrayList}.
     *
     * @return list of all customers
     * @throws SQLException if a database error occurs
     */
    public List<Customer> getAllCustomers() throws SQLException {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT customer_id, nic_passport, full_name, contact_number, email FROM customers";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                customers.add(mapRow(rs));
            }
        }
        return customers;
    }

    /**
     * Finds a customer by database primary key.
     */
    public Customer getCustomerById(int customerId) throws SQLException {
        String sql = "SELECT customer_id, nic_passport, full_name, contact_number, email FROM customers WHERE customer_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Finds a customer by unique NIC/Passport number.
     */
    public Customer getCustomerByNicPassport(String nicPassport) throws SQLException {
        String sql = "SELECT customer_id, nic_passport, full_name, contact_number, email FROM customers WHERE nic_passport = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nicPassport);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getInt("customer_id"),
                rs.getString("nic_passport"),
                rs.getString("full_name"),
                rs.getString("contact_number"),
                rs.getString("email")
        );
    }
}
