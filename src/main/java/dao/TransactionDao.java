package dao;

import model.Transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface TransactionDao {
    void save(Transaction transaction) throws SQLException;
    void save(Connection conn, Transaction transaction) throws SQLException;

    List<Transaction> findByAccountNumber(String accountNumber) throws SQLException;

    List<Transaction> findRecentTransactions(String accountNumber, int limit) throws SQLException;


}
