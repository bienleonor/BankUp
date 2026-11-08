package dao.impl;

import config.DBConnection;
import dao.TransactionDao;
import model.Transaction;
import model.TransactionType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionDAOImp implements TransactionDao {

    private static final Logger LOGGER = Logger.getLogger(TransactionDAOImp.class.getName());


//  ==============================
//  |  SQL CONSTANTS
//  ==============================

    /**
     * Insert Transaction
     */
    String TRANSACT_INSERT =
            "INSERT INTO Transactions (transaction_references, account_number, transaction_type, amount, balance, reference_account, created_at) VALUES(?, ?, ?, ?, ?, ?, NOW())";

    String SQL_FIND_BY_ACCOUNT =
            "SELECT id, transaction_references, account_number, transaction_type, amount, balance, reference_account, created_at FROM transactions WHERE account_number = ?";

    String SQL_FIND_RECENT =
            "SELECT id, account_number, transaction_type, amount, balance_after, " +
                    "       reference_number, remarks, created_at " +
                    "FROM transactions " +
                    "WHERE account_number = ? " +
                    "ORDER BY created_at DESC " +
                    "LIMIT ?";


//  ==============================
//  |  IMPLEMENTATIONS
//  ==============================

    @Override
    public void save(Transaction transaction) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            save(conn, transaction);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save transaction: " + transaction.getReferenceAccount(), e);
            throw e;
        }
    }

    /**
     * {@inheritDoc} Uses the caller's connection for atomic multi-leg operations.
     */
    @Override
    public void save(Connection conn, Transaction transaction) throws SQLException {
        try (PreparedStatement preparedStatement = conn.prepareStatement(TRANSACT_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setString(1, transaction.getAccountNumber());
            preparedStatement.setString(2, transaction.getTransactionType().name());
            preparedStatement.setBigDecimal(3, transaction.getAmount());
            preparedStatement.setBigDecimal(4, transaction.getBalance());
            preparedStatement.setString(5, transaction.getReferenceAccount());
            preparedStatement.setString(6, transaction.getRemarks());
            preparedStatement.executeUpdate();

            // Populate the generated PK back onto the entity
            try (ResultSet keys = preparedStatement.getGeneratedKeys()) {
                if (keys.next()) {
                    transaction.setTransactionId(keys.getLong(1));
                }
            }

            LOGGER.fine("Transaction saved: " + transaction.getReferenceAccount());
        }
    }

    @Override
    public List<Transaction> findByAccountNumber(String accountNumber) throws SQLException {
        List<Transaction> results = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement prepareStatement = conn.prepareStatement(SQL_FIND_BY_ACCOUNT)) {

            prepareStatement.setString(1, accountNumber);

            try (ResultSet resultSet = prepareStatement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(mapRow(resultSet));
                }
            }
            return results;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to fetch transactions for: " + accountNumber, e);
            throw e;
        }
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction txn = new Transaction();
        txn.setTransactionId(rs.getLong("id"));
        txn.setAccountNumber(rs.getString("account_number"));
        txn.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type")));
        txn.setAmount(rs.getBigDecimal("amount"));
        txn.setBalance(rs.getBigDecimal("balance"));
        txn.setReferenceAccount(rs.getString("reference_account"));
        txn.setRemarks(rs.getString("remarks"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            txn.setCreatedAt(createdAt.toLocalDateTime());
        }

        return txn;
    }

    @Override
    public List<Transaction> findRecentTransactions(String accountNumber, int limit)
            throws SQLException {

        List<Transaction> results = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_RECENT)) {

            ps.setString(1, accountNumber);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
            return results;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                    "Failed to fetch recent transactions for: " + accountNumber, e);
            throw e;
        }
    }
}
