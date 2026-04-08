-- ============================================================
-- Bofalgan Pharmaceuticals - MySQL Database Setup Script
-- Run this once before first launch
-- ============================================================

-- Create the database
CREATE DATABASE IF NOT EXISTS bofalgan_pharmacy
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Create the application user
CREATE USER IF NOT EXISTS 'bofalgan'@'localhost'
    IDENTIFIED BY 'BofalganDB@2024';

-- Grant full access
GRANT ALL PRIVILEGES ON bofalgan_pharmacy.* TO 'bofalgan'@'localhost';

FLUSH PRIVILEGES;

-- Switch to the database
USE bofalgan_pharmacy;

-- Verify
SELECT 'Database setup complete. Tables will be created on first application launch.' AS status;
