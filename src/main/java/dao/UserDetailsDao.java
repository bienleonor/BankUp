package dao;

import model.UserDetails;

import java.sql.Connection;
import java.sql.SQLException;

public interface UserDetailsDao {
    void insertUserDetails(UserDetails details) throws SQLException;
    void insertUserDetails(Connection conn, UserDetails details) throws SQLException;
    void updateUserDetails(UserDetails details) throws SQLException;
    UserDetails findUserDetailsByUserId(int userId) throws SQLException;

}
