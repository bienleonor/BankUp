package service;

import config.DBConnection;
import dao.UserDao;
import dao.UserDetailsDao;
import dao.impl.UserDAOImp;
import dao.impl.UserDetailsDAOImp;

import model.User;
import model.UserDetails;
import util.PasswordUtil;
import util.Session;

import java.sql.Connection;
import java.sql.SQLException;

public class UserService {
    private final UserDao userDao = new UserDAOImp();
    private final UserDetailsDao detailsDao = new UserDetailsDAOImp();

    public void register(User user, String rawPassword, UserDetails details) throws SQLException {
        if (userDao.countByUsername(user.getUsername()) > 0)
            throw new IllegalArgumentException("Username already taken");
        if (userDao.countByEmail(user.getEmail()) > 0)
            throw new IllegalArgumentException("Email already registered");
        if (rawPassword == null || rawPassword.length() < 8)
            throw new IllegalArgumentException("Password must be at least 8 characters");

        user.setPassword(PasswordUtil.hash(rawPassword));

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                userDao.insertUser(conn, user);
                details.setUserId(user.getId());
                detailsDao.insertUserDetails(conn, details);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public User login(String username, String rawPassword) throws SQLException {
        User user = userDao.findUserByUsername(username);
        if (user == null || !PasswordUtil.verify(rawPassword, user.getPassword()))
            throw new IllegalArgumentException("Invalid username or password");
        Session.setCurrentUser(user);
        return user;
    }
}