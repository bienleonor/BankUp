package dao;

import model.Account;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface AccountDao {
    void insertAccount(Account account) throws SQLException;
    void insertAccount(Connection conn, Account account) throws SQLException;
    void updateAccountBalance(String accountNumber, BigDecimal balance) throws SQLException;

    Optional<Account> findAccountByNumber (String accountNumber) throws SQLException ;

    List<Account> findAllAccounts() throws SQLException;

    void updateAccountBalance(Connection conn, String accountNumber, BigDecimal balance) throws SQLException;

}
