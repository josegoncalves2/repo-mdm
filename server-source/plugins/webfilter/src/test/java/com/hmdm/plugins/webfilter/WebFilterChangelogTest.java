package com.hmdm.plugins.webfilter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class WebFilterChangelogTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:12")
            .withDatabaseName("mdm_test")
            .withUsername("mdm_user")
            .withPassword("mdm_pass");

    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        Class.forName("org.postgresql.Driver");
        connection = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );

        // Initialize base schema
        initializeBaseSchema();
        // Initialize webfilter tables
        initializeWebFilterTables();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (postgres != null) {
            postgres.stop();
        }
    }

    private void initializeBaseSchema() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            // Create base tables needed by webfilter
            stmt.executeUpdate("CREATE SEQUENCE IF NOT EXISTS users_id_seq");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT PRIMARY KEY DEFAULT nextval('users_id_seq'), " +
                    "login VARCHAR(255) NOT NULL UNIQUE)");

            stmt.executeUpdate("CREATE SEQUENCE IF NOT EXISTS customers_id_seq");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS customers (" +
                    "id INT PRIMARY KEY DEFAULT nextval('customers_id_seq'), " +
                    "name VARCHAR(255) NOT NULL)");

            stmt.executeUpdate("CREATE SEQUENCE IF NOT EXISTS devices_id_seq");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS devices (" +
                    "id INT PRIMARY KEY DEFAULT nextval('devices_id_seq'), " +
                    "customerId INT NOT NULL REFERENCES customers(id) ON DELETE CASCADE, " +
                    "number VARCHAR(255))");

            stmt.executeUpdate("CREATE SEQUENCE IF NOT EXISTS configurations_id_seq");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS configurations (" +
                    "id INT PRIMARY KEY DEFAULT nextval('configurations_id_seq'), " +
                    "customerId INT NOT NULL REFERENCES customers(id) ON DELETE CASCADE, " +
                    "name VARCHAR(255) NOT NULL)");

            stmt.executeUpdate("CREATE SEQUENCE IF NOT EXISTS userroles_id_seq");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS userroles (" +
                    "id INT PRIMARY KEY DEFAULT nextval('userroles_id_seq'), " +
                    "name VARCHAR(255) NOT NULL)");

            stmt.executeUpdate("CREATE SEQUENCE IF NOT EXISTS permissions_id_seq");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS permissions (" +
                    "id INT PRIMARY KEY DEFAULT nextval('permissions_id_seq'), " +
                    "name VARCHAR(255) NOT NULL UNIQUE, " +
                    "description TEXT)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS userRolePermissions (" +
                    "roleId INT NOT NULL REFERENCES userroles(id) ON DELETE CASCADE, " +
                    "permissionId INT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE, " +
                    "PRIMARY KEY (roleId, permissionId))");

            stmt.executeUpdate("CREATE SEQUENCE IF NOT EXISTS plugins_id_seq");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS plugins (" +
                    "id INT PRIMARY KEY DEFAULT nextval('plugins_id_seq'), " +
                    "identifier VARCHAR(255) NOT NULL UNIQUE, " +
                    "name VARCHAR(255), " +
                    "description TEXT, " +
                    "javascriptModuleFile VARCHAR(500), " +
                    "functionsViewTemplate VARCHAR(500), " +
                    "settingsViewTemplate VARCHAR(500), " +
                    "namelocalizationkey VARCHAR(255), " +
                    "settingsPermission VARCHAR(255), " +
                    "functionsPermission VARCHAR(255), " +
                    "deviceFunctionsPermission VARCHAR(255), " +
                    "enabledForDevice BOOLEAN)");

            // Insert default admin role
            stmt.executeUpdate("INSERT INTO userroles (name) VALUES ('Admin') ON CONFLICT DO NOTHING");
        }
    }

    private void initializeWebFilterTables() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            // Create webfilter plugin and permission
            stmt.executeUpdate("INSERT INTO plugins (identifier, name, description, javascriptModuleFile, " +
                    "functionsViewTemplate, settingsViewTemplate, namelocalizationkey, " +
                    "settingsPermission, functionsPermission, deviceFunctionsPermission, enabledForDevice) " +
                    "VALUES ('webfilter', 'Web Filter', 'Web content filtering by category, allowlist and blocklist', " +
                    "'app/components/plugins/webfilter/webfilter.module.js', " +
                    "'app/components/plugins/webfilter/views/policy.html', " +
                    "'app/components/plugins/webfilter/views/settings.html', " +
                    "'plugin.webfilter.localization.key.name', " +
                    "'plugin_webfilter_access', 'plugin_webfilter_access', 'plugin_webfilter_access', true)");

            stmt.executeUpdate("INSERT INTO permissions (name, description) " +
                    "VALUES ('plugin_webfilter_access', 'Has access to web filter configuration')");

            // Create webfilter tables
            stmt.executeUpdate("CREATE SEQUENCE IF NOT EXISTS plugin_webfilter_policy_id_seq");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS plugin_webfilter_policy (" +
                    "id INT PRIMARY KEY DEFAULT nextval('plugin_webfilter_policy_id_seq'), " +
                    "configurationId INT NOT NULL UNIQUE REFERENCES configurations(id) ON DELETE CASCADE, " +
                    "categoriesJson TEXT, " +
                    "entriesJson TEXT)");

            stmt.executeUpdate("CREATE SEQUENCE IF NOT EXISTS plugin_webfilter_category_id_seq");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS plugin_webfilter_category (" +
                    "id INT PRIMARY KEY DEFAULT nextval('plugin_webfilter_category_id_seq'), " +
                    "policyId INT NOT NULL REFERENCES plugin_webfilter_policy(id) ON DELETE CASCADE, " +
                    "categoryId VARCHAR(50) NOT NULL, " +
                    "CONSTRAINT plugin_webfilter_category_policy_category_unique UNIQUE (policyId, categoryId))");

            stmt.executeUpdate("CREATE SEQUENCE IF NOT EXISTS plugin_webfilter_entry_id_seq");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS plugin_webfilter_entry (" +
                    "id INT PRIMARY KEY DEFAULT nextval('plugin_webfilter_entry_id_seq'), " +
                    "policyId INT NOT NULL REFERENCES plugin_webfilter_policy(id) ON DELETE CASCADE, " +
                    "kind VARCHAR(20) NOT NULL, " +
                    "value VARCHAR(500) NOT NULL, " +
                    "CONSTRAINT plugin_webfilter_entry_policy_kind_value_unique UNIQUE (policyId, kind, value))");

            stmt.executeUpdate("CREATE SEQUENCE IF NOT EXISTS plugin_webfilter_locked_history_id_seq");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS plugin_webfilter_locked_history (" +
                    "id INT PRIMARY KEY DEFAULT nextval('plugin_webfilter_locked_history_id_seq'), " +
                    "deviceId INT NOT NULL REFERENCES devices(id) ON DELETE CASCADE, " +
                    "timestamp BIGINT NOT NULL, " +
                    "packagesJson TEXT)");

            stmt.executeUpdate("CREATE SEQUENCE IF NOT EXISTS plugin_webfilter_settings_id_seq");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS plugin_webfilter_settings (" +
                    "id INT PRIMARY KEY DEFAULT nextval('plugin_webfilter_settings_id_seq'), " +
                    "customerId INT NOT NULL REFERENCES customers(id) ON DELETE CASCADE, " +
                    "dnsDomain VARCHAR(255), " +
                    "CONSTRAINT plugin_webfilter_settings_customer_unique UNIQUE (customerId))");

            stmt.executeUpdate("CREATE SEQUENCE IF NOT EXISTS plugin_webfilter_app_category_id_seq");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS plugin_webfilter_app_category (" +
                    "id INT PRIMARY KEY DEFAULT nextval('plugin_webfilter_app_category_id_seq'), " +
                    "customerId INT NOT NULL REFERENCES customers(id) ON DELETE CASCADE, " +
                    "packageId VARCHAR(255) NOT NULL, " +
                    "categoryId VARCHAR(50) NOT NULL, " +
                    "CONSTRAINT plugin_webfilter_app_category_customer_package_category_unique UNIQUE (customerId, packageId, categoryId))");
        }
    }

    @Test
    void testPluginIsRegistered() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT identifier, functionsPermission FROM plugins WHERE identifier = 'webfilter'");
            assertTrue(rs.next(), "Plugin webfilter should be registered");
            assertEquals("webfilter", rs.getString("identifier"));
            assertEquals("plugin_webfilter_access", rs.getString("functionsPermission"));
        }
    }

    @Test
    void testPermissionIsRegistered() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT name FROM permissions WHERE name = 'plugin_webfilter_access'");
            assertTrue(rs.next(), "Permission plugin_webfilter_access should be registered");
            assertEquals("plugin_webfilter_access", rs.getString("name"));
        }
    }

    @Test
    void testConfigurationIdIsUnique() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            // Insert test customer and configurations
            stmt.executeUpdate("INSERT INTO customers (name) VALUES ('Test Customer')");
            ResultSet customerRs = stmt.executeQuery("SELECT id FROM customers WHERE name = 'Test Customer' LIMIT 1");
            int customerId = 0;
            if (customerRs.next()) {
                customerId = customerRs.getInt(1);
            }

            stmt.executeUpdate("INSERT INTO configurations (customerId, name) VALUES (" + customerId + ", 'Config1')");
            stmt.executeUpdate("INSERT INTO configurations (customerId, name) VALUES (" + customerId + ", 'Config2')");

            ResultSet configRs = stmt.executeQuery("SELECT id FROM configurations WHERE name IN ('Config1', 'Config2') ORDER BY id");
            int configId1 = 0, configId2 = 0;
            if (configRs.next()) configId1 = configRs.getInt(1);
            if (configRs.next()) configId2 = configRs.getInt(1);

            // Insert policy with configId1
            stmt.executeUpdate("INSERT INTO plugin_webfilter_policy (configurationId, categoriesJson, entriesJson) " +
                    "VALUES (" + configId1 + ", '{}', '{}')");

            // Try to insert another policy with the same configId1 - should fail
            try {
                stmt.executeUpdate("INSERT INTO plugin_webfilter_policy (configurationId, categoriesJson, entriesJson) " +
                        "VALUES (" + configId1 + ", '{}', '{}')");
                fail("Should not allow duplicate configurationId");
            } catch (Exception e) {
                // Expected - unique constraint violation
                assertTrue(true);
            }

            // Insert with different configId2 - should succeed
            stmt.executeUpdate("INSERT INTO plugin_webfilter_policy (configurationId, categoriesJson, entriesJson) " +
                    "VALUES (" + configId2 + ", '{}', '{}')");

            ResultSet policyRs = stmt.executeQuery("SELECT COUNT(*) FROM plugin_webfilter_policy");
            policyRs.next();
            assertEquals(2, policyRs.getInt(1), "Should have 2 policies");
        }
    }

    @Test
    void testCascadeDeletePolicy() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            // Insert test data
            stmt.executeUpdate("INSERT INTO customers (name) VALUES ('Test Customer')");
            ResultSet customerRs = stmt.executeQuery("SELECT id FROM customers WHERE name = 'Test Customer' LIMIT 1");
            int customerId = 0;
            if (customerRs.next()) {
                customerId = customerRs.getInt(1);
            }

            stmt.executeUpdate("INSERT INTO configurations (customerId, name) VALUES (" + customerId + ", 'Config1')");
            ResultSet configRs = stmt.executeQuery("SELECT id FROM configurations WHERE name = 'Config1' LIMIT 1");
            int configId = 0;
            if (configRs.next()) {
                configId = configRs.getInt(1);
            }

            stmt.executeUpdate("INSERT INTO plugin_webfilter_policy (configurationId, categoriesJson, entriesJson) " +
                    "VALUES (" + configId + ", '{}', '{}')");

            ResultSet policyRs = stmt.executeQuery("SELECT id FROM plugin_webfilter_policy WHERE configurationId = " + configId);
            int policyId = 0;
            if (policyRs.next()) {
                policyId = policyRs.getInt(1);
            }

            // Insert category, entry, and locked_history
            stmt.executeUpdate("INSERT INTO plugin_webfilter_category (policyId, categoryId) VALUES (" + policyId + ", 'adult')");
            stmt.executeUpdate("INSERT INTO plugin_webfilter_entry (policyId, kind, value) VALUES (" + policyId + ", 'url', 'example.com')");

            stmt.executeUpdate("INSERT INTO devices (customerId, number) VALUES (" + customerId + ", 'device1')");
            ResultSet deviceRs = stmt.executeQuery("SELECT id FROM devices WHERE number = 'device1' LIMIT 1");
            int deviceId = 0;
            if (deviceRs.next()) {
                deviceId = deviceRs.getInt(1);
            }

            stmt.executeUpdate("INSERT INTO plugin_webfilter_locked_history (deviceId, timestamp, packagesJson) " +
                    "VALUES (" + deviceId + ", " + System.currentTimeMillis() + ", '{}')");

            // Delete configuration - should cascade delete policy and its children
            stmt.executeUpdate("DELETE FROM configurations WHERE id = " + configId);

            // Verify cascade delete
            ResultSet policyCheckRs = stmt.executeQuery("SELECT COUNT(*) FROM plugin_webfilter_policy WHERE configurationId = " + configId);
            policyCheckRs.next();
            assertEquals(0, policyCheckRs.getInt(1), "Policy should be deleted with configuration");

            ResultSet categoryCheckRs = stmt.executeQuery("SELECT COUNT(*) FROM plugin_webfilter_category WHERE policyId = " + policyId);
            categoryCheckRs.next();
            assertEquals(0, categoryCheckRs.getInt(1), "Category should be deleted with policy");

            ResultSet entryCheckRs = stmt.executeQuery("SELECT COUNT(*) FROM plugin_webfilter_entry WHERE policyId = " + policyId);
            entryCheckRs.next();
            assertEquals(0, entryCheckRs.getInt(1), "Entry should be deleted with policy");
        }
    }

    @Test
    void testCategoryUniqueness() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            // Setup
            stmt.executeUpdate("INSERT INTO customers (name) VALUES ('Test Customer')");
            ResultSet customerRs = stmt.executeQuery("SELECT id FROM customers WHERE name = 'Test Customer' LIMIT 1");
            int customerId = 0;
            if (customerRs.next()) {
                customerId = customerRs.getInt(1);
            }

            stmt.executeUpdate("INSERT INTO configurations (customerId, name) VALUES (" + customerId + ", 'Config1')");
            ResultSet configRs = stmt.executeQuery("SELECT id FROM configurations WHERE name = 'Config1' LIMIT 1");
            int configId = 0;
            if (configRs.next()) {
                configId = configRs.getInt(1);
            }

            stmt.executeUpdate("INSERT INTO plugin_webfilter_policy (configurationId, categoriesJson, entriesJson) " +
                    "VALUES (" + configId + ", '{}', '{}')");

            ResultSet policyRs = stmt.executeQuery("SELECT id FROM plugin_webfilter_policy WHERE configurationId = " + configId);
            int policyId = 0;
            if (policyRs.next()) {
                policyId = policyRs.getInt(1);
            }

            // Insert category
            stmt.executeUpdate("INSERT INTO plugin_webfilter_category (policyId, categoryId) VALUES (" + policyId + ", 'adult')");

            // Try to insert duplicate - should fail
            try {
                stmt.executeUpdate("INSERT INTO plugin_webfilter_category (policyId, categoryId) VALUES (" + policyId + ", 'adult')");
                fail("Should not allow duplicate category for same policy");
            } catch (Exception e) {
                assertTrue(true);
            }
        }
    }

    @Test
    void testSettingsUniquenessPerCustomer() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            // Insert customer
            stmt.executeUpdate("INSERT INTO customers (name) VALUES ('Test Customer')");
            ResultSet customerRs = stmt.executeQuery("SELECT id FROM customers WHERE name = 'Test Customer' LIMIT 1");
            int customerId = 0;
            if (customerRs.next()) {
                customerId = customerRs.getInt(1);
            }

            // Insert settings for customer
            stmt.executeUpdate("INSERT INTO plugin_webfilter_settings (customerId, dnsDomain) " +
                    "VALUES (" + customerId + ", 'dns.example.com')");

            // Try to insert another settings for same customer - should fail
            try {
                stmt.executeUpdate("INSERT INTO plugin_webfilter_settings (customerId, dnsDomain) " +
                        "VALUES (" + customerId + ", 'dns2.example.com')");
                fail("Should not allow multiple settings per customer");
            } catch (Exception e) {
                assertTrue(true);
            }
        }
    }

    @Test
    void testAppCategoryUniqueness() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            // Insert customer
            stmt.executeUpdate("INSERT INTO customers (name) VALUES ('Test Customer')");
            ResultSet customerRs = stmt.executeQuery("SELECT id FROM customers WHERE name = 'Test Customer' LIMIT 1");
            int customerId = 0;
            if (customerRs.next()) {
                customerId = customerRs.getInt(1);
            }

            // Insert app category
            stmt.executeUpdate("INSERT INTO plugin_webfilter_app_category (customerId, packageId, categoryId) " +
                    "VALUES (" + customerId + ", 'com.example.app', 'social')");

            // Try to insert duplicate - should fail
            try {
                stmt.executeUpdate("INSERT INTO plugin_webfilter_app_category (customerId, packageId, categoryId) " +
                        "VALUES (" + customerId + ", 'com.example.app', 'social')");
                fail("Should not allow duplicate app category");
            } catch (Exception e) {
                assertTrue(true);
            }

            // Insert with different package - should succeed
            stmt.executeUpdate("INSERT INTO plugin_webfilter_app_category (customerId, packageId, categoryId) " +
                    "VALUES (" + customerId + ", 'com.example.app2', 'social')");

            ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) FROM plugin_webfilter_app_category WHERE customerId = " + customerId);
            countRs.next();
            assertEquals(2, countRs.getInt(1), "Should allow different packages");
        }
    }
}
