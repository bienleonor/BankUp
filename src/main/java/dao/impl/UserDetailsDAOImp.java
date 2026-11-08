package dao.impl;

import config.DBConnection;
import dao.UserDetailsDao;
import model.UserDetails;

import java.sql.*;

public class UserDetailsDAOImp implements UserDetailsDao {

    String USER_DETAILS_INSERT =
            "INSERT INTO user_details(user_id, first_name, middle_name, last_name, birthdate, created_at) VALUES(?, ?, ?, ?, ?, NOW())";

    String UPDATE_USER_DETAILS =
            "UPDATE user_details SET first_name, middle_name, last_name, birthdate VALUES(?, ?, ?, ?) WHERE user_id = ?";

    String SQL_FIND_USER_DETAILS =
            "SELECT first_name, middle_name, last_name, birthdate, created_at FROM user_details WHERE user_id = ?";

    @Override
    public void insertUserDetails(Connection conn, UserDetails details) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(USER_DETAILS_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, details.getUserId());
            ps.setString(2, details.getFirstName());
            ps.setString(3, details.getMiddleName());   // may be null
            ps.setString(4, details.getLastName());
            ps.setDate(5, java.sql.Date.valueOf(details.getBirthdate()));
            ps.executeUpdate();

        }
    }

    @Override
    public void insertUserDetails(UserDetails details) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            insertUserDetails(conn, details);
        }
    }

    @Override
    public void updateUserDetails(UserDetails details) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_USER_DETAILS)) {
            ps.setString(1, details.getFirstName());
            ps.setString(2, details.getMiddleName());
            ps.setString(3, details.getLastName());
            ps.setDate(4, java.sql.Date.valueOf(details.getBirthdate()));
            ps.setLong(5, details.getUserId());

            if (ps.executeUpdate() == 0) {
                throw new SQLException("No user details found for user_id " + details.getUserId());
            }
        }
    }

    @Override
    public UserDetails findUserDetailsByUserId(int userId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_USER_DETAILS)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapUserDetails(rs) : null;
            }
        }
    }

    private UserDetails mapUserDetails(ResultSet rs) throws SQLException {
        UserDetails d = new UserDetails();
        d.setUserId(rs.getLong("user_id"));
        d.setFirstName(rs.getString("first_name"));
        d.setMiddleName(rs.getString("middle_name"));
        d.setLastName(rs.getString("last_name"));
        d.setBirthdate(rs.getDate("birthdate").toLocalDate());
        d.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return d;
    }
}
