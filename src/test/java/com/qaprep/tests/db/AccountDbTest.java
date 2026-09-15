package com.qaprep.tests.db;

import com.qaprep.framework.db.DbConnectionManager;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Verifies the accounts/transactions schema directly at the database layer,
 * independent of any UI or API surface. Runs against a real MySQL instance
 * (a service container in CI, or a local MySQL install) rather than an
 * embedded database, so the schema/queries under test behave the same way
 * they would against production infrastructure.
 * <p>
 * schema.sql and seed.sql reset the tables before each run, so this class
 * owns its own data and does not depend on execution order relative to the
 * UI or API suites.
 * <p>
 * {@code singleThreaded = true} forces this class's @Test methods onto one
 * thread even though the suite runs parallel="methods". Every method shares
 * a single Connection field, and insertedTransactionIsVisibleThenRolledBack
 * toggles that connection's autocommit/rollback state — letting another
 * method interleave on the same connection would corrupt both.
 */
@Test(singleThreaded = true)
public class AccountDbTest {

    private Connection connection;

    @BeforeClass
    public void setUpDatabase() throws SQLException {
        connection = DbConnectionManager.getConnection();
        runScript("db/schema.sql");
        runScript("db/seed.sql");
    }

    @AfterClass
    public void tearDownDatabase() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void activeAccountReturnsCorrectBalanceAndStatus() throws SQLException {
        String sql = "SELECT balance, status FROM accounts WHERE account_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "ACC1001");

            try (ResultSet resultSet = statement.executeQuery()) {
                Assert.assertTrue(resultSet.next(), "Expected a row for ACC1001");
                Assert.assertEquals(resultSet.getBigDecimal("balance"), new BigDecimal("5000.00"));
                Assert.assertEquals(resultSet.getString("status"), "ACTIVE");
            }
        }
    }

    @Test
    public void closedAccountHasZeroBalance() throws SQLException {
        String sql = "SELECT balance, status FROM accounts WHERE account_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "ACC1003");

            try (ResultSet resultSet = statement.executeQuery()) {
                Assert.assertTrue(resultSet.next(), "Expected a row for ACC1003");
                Assert.assertEquals(resultSet.getBigDecimal("balance"), new BigDecimal("0.00"));
                Assert.assertEquals(resultSet.getString("status"), "CLOSED");
            }
        }
    }

    @Test
    public void activeAccountTransactionCountMatchesSeedData() throws SQLException {
        String sql = "SELECT COUNT(*) AS transaction_count FROM transactions WHERE account_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "ACC1001");

            try (ResultSet resultSet = statement.executeQuery()) {
                Assert.assertTrue(resultSet.next());
                Assert.assertEquals(resultSet.getInt("transaction_count"), 2,
                        "ACC1001 should have exactly the 2 transactions seeded for it");
            }
        }
    }

    @Test
    public void unknownAccountIdReturnsNoRows() throws SQLException {
        String sql = "SELECT * FROM accounts WHERE account_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "ACC-DOES-NOT-EXIST");

            try (ResultSet resultSet = statement.executeQuery()) {
                Assert.assertFalse(resultSet.next(), "A non-existent account_id must return zero rows");
            }
        }
    }

    /**
     * Inserts a transaction and confirms it is visible in the same connection,
     * then rolls back so the insert never becomes part of the seeded fixture
     * other tests in this class rely on. setAutoCommit(false) + rollback() is
     * the explicit transaction boundary that keeps this test's side effect from
     * leaking into activeAccountTransactionCountMatchesSeedData above.
     */
    @Test
    public void insertedTransactionIsVisibleThenRolledBack() throws SQLException {
        connection.setAutoCommit(false);

        try {
            String insertSql = "INSERT INTO transactions (account_id, amount, transaction_type) VALUES (?, ?, ?)";
            try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
                insert.setString(1, "ACC1002");
                insert.setBigDecimal(2, new BigDecimal("75.00"));
                insert.setString(3, "DEBIT");

                int rowsInserted = insert.executeUpdate();
                Assert.assertEquals(rowsInserted, 1);
            }

            String countSql = "SELECT COUNT(*) AS transaction_count FROM transactions WHERE account_id = ?";
            try (PreparedStatement count = connection.prepareStatement(countSql)) {
                count.setString(1, "ACC1002");

                try (ResultSet resultSet = count.executeQuery()) {
                    Assert.assertTrue(resultSet.next());
                    Assert.assertEquals(resultSet.getInt("transaction_count"), 2,
                            "ACC1002 should now show its original seeded transaction plus this one");
                }
            }
        } finally {
            connection.rollback();
            connection.setAutoCommit(true);
        }
    }

    /**
     * Reads a .sql resource and executes each ;-terminated statement in order.
     * Deliberately simple (no external migration library) since the schema
     * here is small and fixed — not a substitute for Flyway/Liquibase on a
     * real project.
     */
    private void runScript(String resourcePath) throws SQLException {
        String script = readResource(resourcePath);

        try (Statement statement = connection.createStatement()) {
            for (String sql : script.split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
        }
    }

    private String readResource(String resourcePath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Resource not found on classpath: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read resource: " + resourcePath, e);
        }
    }
}
