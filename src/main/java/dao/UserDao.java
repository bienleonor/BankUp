package dao;

import model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface UserDao {
    void insertUser(User user) throws SQLException;                  // sets generated id on user
    void insertUser(Connection conn, User user) throws SQLException; // for transactions
    User findUserById(int id) throws SQLException;
    User findUserByUsername(String username) throws SQLException;    // null if not found
    List<User> findAll() throws SQLException;
    int countByUsername(String username) throws SQLException;
    int countByEmail(String email) throws SQLException;
}
