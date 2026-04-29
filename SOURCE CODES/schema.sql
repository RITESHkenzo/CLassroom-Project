-- ============================================================
-- Cafeteria Order and Billing System
-- Database Schema — Phase 3
-- Run this file in MySQL to set up the database
-- ============================================================

CREATE DATABASE IF NOT EXISTS cafeteria_db;
USE cafeteria_db;

-- ── USERS ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    user_id   INT AUTO_INCREMENT PRIMARY KEY,
    username  VARCHAR(50)  UNIQUE NOT NULL,
    password  VARCHAR(100) NOT NULL,          -- store plain text for now; use hashing in production
    role      ENUM('ADMIN','OPERATOR') NOT NULL
);

-- Seed default accounts
INSERT INTO users (username, password, role) VALUES
    ('admin',    'admin123',    'ADMIN'),
    ('operator', 'operator123', 'OPERATOR');

-- ── MENU ITEMS ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS menu_items (
    item_id   INT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(100) UNIQUE NOT NULL,
    category  VARCHAR(50)  NOT NULL,
    price     DECIMAL(10,2) NOT NULL CHECK (price > 0),
    available BOOLEAN DEFAULT TRUE
);

-- Seed sample menu
INSERT INTO menu_items (name, category, price, available) VALUES
    ('Masala Chai',      'Beverages', 15.00, TRUE),
    ('Samosa',           'Snacks',    20.00, TRUE),
    ('Veg Thali',        'Meals',     70.00, TRUE),
    ('Paneer Sandwich',  'Snacks',    45.00, TRUE),
    ('Cold Coffee',      'Beverages', 40.00, TRUE),
    ('Dal Rice',         'Meals',     55.00, TRUE);

-- ── CUSTOMERS ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    phone       VARCHAR(20)
);

-- ── ORDERS ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
    order_id        INT AUTO_INCREMENT PRIMARY KEY,
    customer_id     INT NOT NULL,
    order_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    status          VARCHAR(20) DEFAULT 'PLACED',
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- ── ORDER ITEMS ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS order_items (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id      INT NOT NULL,
    item_id       INT NOT NULL,
    quantity      INT NOT NULL CHECK (quantity > 0),
    unit_price    DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (item_id)  REFERENCES menu_items(item_id)
);

-- ── BILLS ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bills (
    bill_id        INT AUTO_INCREMENT PRIMARY KEY,
    order_id       INT UNIQUE NOT NULL,
    subtotal       DECIMAL(10,2) NOT NULL,
    tax_amount     DECIMAL(10,2) NOT NULL,
    total_amount   DECIMAL(10,2) NOT NULL,
    bill_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);
