-- ============================================================================
-- SmartStay Hotel Booking System - MySQL Database Schema & Seed Data
-- Module: CI6115 Programming III - Patterns and Algorithms
-- ============================================================================

CREATE DATABASE IF NOT EXISTS smartstay_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE smartstay_db;

-- ----------------------------------------------------------------------------
-- 1. Customers Table
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    nic_passport VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(120) NOT NULL,
    contact_number VARCHAR(30) NOT NULL,
    email VARCHAR(120) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- 2. Rooms Table
-- Single-Table Inheritance storage for Room subclasses:
-- StandardRoom, DeluxeRoom, SuiteRoom, FamilySuiteRoom
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rooms (
    room_id VARCHAR(20) PRIMARY KEY,
    room_type ENUM('STANDARD', 'DELUXE', 'SUITE', 'FAMILY_SUITE') NOT NULL,
    bed_type ENUM('SINGLE', 'DOUBLE', 'QUEEN', 'KING') NOT NULL,
    daily_rate DECIMAL(10, 2) NOT NULL,
    base_capacity INT NOT NULL,
    max_capacity INT NOT NULL,
    complimentary_breakfast BOOLEAN NOT NULL,
    extra_guest_charge DECIMAL(10, 2) NOT NULL,
    tax_rate DECIMAL(5, 2) NOT NULL,
    availability_status ENUM('AVAILABLE', 'RESERVED', 'UNDER_MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE'
);

-- ----------------------------------------------------------------------------
-- 3. Reservations Table
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reservations (
    reservation_id VARCHAR(20) PRIMARY KEY,
    customer_id INT NOT NULL,
    room_id VARCHAR(20) NOT NULL,
    booking_date DATE NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    num_guests INT NOT NULL,
    booking_advance DECIMAL(10, 2) NOT NULL DEFAULT 10000.00,
    reservation_status ENUM('CONFIRMED', 'CANCELLED', 'COMPLETED') NOT NULL DEFAULT 'CONFIRMED',
    CONSTRAINT fk_res_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_res_room FOREIGN KEY (room_id) REFERENCES rooms(room_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

-- ----------------------------------------------------------------------------
-- 4. Invoices Table (1-to-1 Composition with Reservation)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS invoices (
    invoice_id VARCHAR(20) PRIMARY KEY,
    reservation_id VARCHAR(20) NOT NULL UNIQUE,
    num_nights INT NOT NULL,
    base_room_charge DECIMAL(10, 2) NOT NULL,
    extra_guest_charge DECIMAL(10, 2) NOT NULL,
    discount_amount DECIMAL(10, 2) NOT NULL,
    tax_amount DECIMAL(10, 2) NOT NULL,
    advance_deducted DECIMAL(10, 2) NOT NULL,
    final_payable_amount DECIMAL(10, 2) NOT NULL,
    refund_due DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id)
        ON UPDATE CASCADE ON DELETE CASCADE
);

-- ----------------------------------------------------------------------------
-- 5. Payments Table (Tracks Advance, Final Settlement, and Refunds)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    reservation_id VARCHAR(20) NOT NULL,
    payment_type ENUM('ADVANCE', 'FINAL_SETTLEMENT', 'REFUND') NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pay_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id)
        ON UPDATE CASCADE ON DELETE CASCADE
);

-- ----------------------------------------------------------------------------
-- Seed Sample Rooms (per Table 1 Specifications & Documented Base Capacities)
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO rooms
    (room_id, room_type, bed_type, daily_rate, base_capacity, max_capacity, complimentary_breakfast, extra_guest_charge, tax_rate, availability_status)
VALUES
    ('R101', 'STANDARD',     'SINGLE', 6000.00,  2, 3, FALSE, 1000.00, 10.00, 'AVAILABLE'),
    ('R102', 'STANDARD',     'DOUBLE', 6000.00,  2, 3, FALSE, 1000.00, 10.00, 'AVAILABLE'),
    ('R201', 'DELUXE',       'DOUBLE', 9000.00,  2, 4, TRUE,  1500.00, 12.00, 'AVAILABLE'),
    ('R202', 'DELUXE',       'QUEEN',  9000.00,  2, 4, TRUE,  1500.00, 12.00, 'AVAILABLE'),
    ('R301', 'SUITE',        'KING',   14000.00, 3, 5, TRUE,  2000.00, 15.00, 'AVAILABLE'),
    ('R401', 'FAMILY_SUITE', 'KING',   18000.00, 4, 6, TRUE,  2500.00, 15.00, 'AVAILABLE'),
    ('R402', 'FAMILY_SUITE', 'QUEEN',  18000.00, 4, 6, TRUE,  2500.00, 15.00, 'UNDER_MAINTENANCE');

-- ----------------------------------------------------------------------------
-- Seed Sample Customers
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO customers (nic_passport, full_name, contact_number, email)
VALUES
    ('199512345678', 'Kasun Perera',      '+94771234567', 'kasun.perera@example.lk'),
    ('N8845123',     'Emily Watson',      '+447911123456', 'emily.watson@example.co.uk');
