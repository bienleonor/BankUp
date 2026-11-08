package util;

import model.User;

public final class Session {

    private static User currentUser;

    private Session() {}

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("Not logged in");
        }
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void logout() {
        currentUser = null;
    }
}