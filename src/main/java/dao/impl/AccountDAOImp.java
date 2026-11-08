package dao.impl;

import config.DBConnection;
import dao.AccountDao;
import model.Account;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AccountDAOImp implements AccountDao {

    private static final Logger LOGGER = Logger.getLogger(AccountDAOImp.class.getName());


    // SQL CONSTANTS
    private static final String ACCOUNT_INSERT =
            "INSERT INTO accounts " +
                    "(user_id, account_number, account_name, balance, created_at) " +
                    "VALUES (?, ?, ?, ?, NOW())";

    private static final String BALANCE_UPDATE =
            "UPDATE accounts SET balance = ? WHERE account_number = ?";

    private static final String SQL_FIND_BY_NUMBER =
            "SELECT account_id, user_id, account_number, account_name, " +
                    "balance, created_at FROM accounts WHERE account_number = ?";

    private static final String SQL_FIND_ALL =
            "SELECT account_id, user_id, account_number, account_name, " +
                    "balance, created_at FROM accounts";

    /**
     * Insert Account
     */

    @Override
    public void insertAccount(Account account) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             ) {
            insertAccount(conn, account);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to create account: " + account.getAccountNumber(), e);
            throw e;
        }
    }

    @Override
    public void insertAccount(Connection conn, Account account) throws SQLException {
        try (PreparedStatement preparedStatement = conn.prepareStatement(
                ACCOUNT_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setLong(1, account.getUserId());
            preparedStatement.setString(2, account.getAccountNumber());
            preparedStatement.setString(3, account.getAccountName());
            preparedStatement.setBigDecimal(4, account.getBalance());

            preparedStatement.executeUpdate();

            try (ResultSet keys = preparedStatement.getGeneratedKeys()) {
                if (keys.next()) {
                    account.setId(keys.getLong(1));
                }
            }

            LOGGER.info("Account created: " + account.getAccountNumber());
        }
    }

    /**
     * Update Balance
     */

    @Override
    public void updateAccountBalance(String accountNumber, BigDecimal balance) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            updateAccountBalance(conn, accountNumber, balance);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update balance for: " + accountNumber, e);
            throw e;
        }
    }

    @Override
    public void updateAccountBalance(Connection conn, String accountNumber, BigDecimal balance)
            throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(BALANCE_UPDATE)) {
            ps.setBigDecimal(1, balance);
            ps.setString(2, accountNumber);
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException("Balance update affected 0 rows for account: " + accountNumber);
            }

            LOGGER.fine("Balance updated for " + accountNumber + " → " + balance);
        }
    }

    /**
     * Find Account By Account Number
     */

    @Override
    public Optional<Account> findAccountByNumber(String accountNumber) throws SQLException {

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement preparedStatement =
                     conn.prepareStatement(SQL_FIND_BY_NUMBER)) {

            preparedStatement.setString(1, accountNumber);

            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapAccount(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to find account: " + accountNumber, e);
            throw e;
        }
    }

    /**
     * FIND ALL ACCOUNT
     */

    @Override
    public List<Account> findAllAccounts() throws SQLException {

        List<Account> accounts = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = preparedStatement.executeQuery()) {

            while (rs.next()) {
                accounts.add(mapAccount(rs));
            }
            return accounts;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to list all accounts", e);
            throw e;
        }
    }

    // MAP RESULT SET TO ACCOUNT

    private Account mapAccount(ResultSet rs) throws SQLException {

        Account account = new Account();

        account.setId(rs.getLong("account_id"));
        account.setUserId(rs.getLong("user_id"));
        account.setAccountNumber(rs.getString("account_number"));
        account.setAccountName(rs.getString("account_name"));
        account.setBalance(rs.getBigDecimal("balance"));

        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");

        if (createdAt != null) {
            account.setCreatedAt(createdAt.toLocalDateTime());
        }

        return account;
    }
}