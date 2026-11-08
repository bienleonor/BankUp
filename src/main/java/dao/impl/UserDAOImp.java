package dao.impl;

import config.DBConnection;
import dao.UserDao;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAOImp implements UserDao {

//  ==============================
//  |  SQL CONSTANTS
//  ==============================

    String USER_INSERT =
            "INSERT INTO username, password, email, created_at VALUES(?, ?, ?, NOW())";

    String SQL_FIND_USER_BY_ID =
            "SELECT id, username, email, created_at FROM users WHERE id = ?";

    String SQL_FIND_ALL_USER =
            "SELECT id, username, email, created_at FROM users";

    String SQL_FIND_BY_USERNAME =
            "SELECT * FROM users_table WHERE username = ?";

    String SQL_COUNT_BY_USERNAME =
            "SELECT COUNT(*) FROM users_table WHERE username = ?";

    //  ==============================
    //  |  IMPLEMENTATIONS
    //  ==============================
    @Override
    public void insertUser(Connection conn, User user) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(USER_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());   // already hashed
            ps.setString(3, user.getEmail());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) user.setId(rs.getLong(1));
            }
        }
    }

    @Override
    public void insertUser(User user) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            insertUser(conn, user);
        }
    }

    @Override
    public User findUserByUsername(String username) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_USERNAME)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setEmail(rs.getString("email"));
        u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return u;
    }

    @Override
    public int countByUsername(String username) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_COUNT_BY_USERNAME)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public int countByEmail(String email) throws SQLException {
        return 0;
    }

    @Override
    public User findUserById(int id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_USER_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        }
    }

    @Override
    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL_USER);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        }
        return users;
    }
}
