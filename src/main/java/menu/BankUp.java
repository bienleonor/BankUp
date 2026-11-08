package menu;

import dao.impl.AccountDAOImp;
import dao.impl.TransactionDAOImp;
import model.User;
import model.UserDetails;
import service.AccountService;
import service.TransactionService;
import service.UserService;
import util.Session;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.io.Console;
import java.sql.SQLException;
import java.util.Scanner;

public class BankUp {

    private final Scanner scanner = new Scanner(System.in);
    private final UserService userService = new UserService();
    private final AccountService accountService = new AccountService(new AccountDAOImp(), scanner);
    private final TransactionService transactionService =
            new TransactionService(new AccountDAOImp(), new TransactionDAOImp(), accountService, scanner);
    private boolean exit;

    @FunctionalInterface
    private interface Action {
        void run() throws SQLException;
    }

    public void start() {
        while (!exit) {
            if (Session.isLoggedIn()) {
                printMenu();
                handleChoice(prompt("Enter choice: "));
            } else {
                showAuthMenu();
            }
        }
        System.out.println("Thank you for banking with us. Goodbye!");
    }

    private void showAuthMenu() {
        System.out.println();
        System.out.println("==================================");
        System.out.println("    BANKING MANAGEMENT SYSTEM     ");
        System.out.println("==================================");
        System.out.println(" 1. Login");
        System.out.println(" 2. Register");
        System.out.println(" 3. Exit");
        System.out.println("==================================");
        switch (prompt("Enter choice: ")) {
            case "1" -> safely(this::login);
            case "2" -> safely(this::register);
            case "3" -> exit = true;
            default -> System.out.println("Invalid choice.");
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("==================================");
        System.out.println("    BANKING MANAGEMENT SYSTEM     ");
        System.out.println("==================================");
        System.out.println(" 1. Create Account");
        System.out.println(" 2. Balance Inquiry");
        System.out.println(" 3. Deposit");
        System.out.println(" 4. Withdraw");
        System.out.println(" 5. Transfer");
        System.out.println(" 6. Transaction History");
        System.out.println(" 7. Mini Statement");
        System.out.println(" 8. List All Accounts");
        System.out.println(" 9. Logout");
        System.out.println("10. Exit");
        System.out.println("==================================");
    }

    private void handleChoice(String choice) {
        switch (choice) {
            case "1" -> safely(accountService::createAccount);
            case "2" -> safely(accountService::balanceInquiry);
            case "3" -> safely(transactionService::deposit);
            case "4" -> safely(transactionService::withdraw);
            case "5" -> safely(transactionService::transfer);
            case "6" -> safely(transactionService::viewTransactionHistory);
            case "7" -> safely(transactionService::miniStatement);
            case "8" -> safely(accountService::listAccounts);
            case "9" -> Session.logout();
            case "10" -> exit = true;
            default -> System.out.println("Invalid choice. Please enter 1-10.");
        }
    }

    private void register() throws SQLException {
        User user = new User();
        user.setUsername(prompt("Username: "));
        user.setEmail(prompt("Email: "));
        if (!user.getEmail().contains("@")) {
            throw new IllegalArgumentException("Invalid email address");
        }

        UserDetails details = new UserDetails();
        details.setFirstName(prompt("First name: "));
        String middle = prompt("Middle name (optional): ");
        details.setMiddleName(middle.isBlank() ? null : middle);
        details.setLastName(prompt("Last name: "));
        details.setBirthdate(readDate("Birthdate (yyyy-MM-dd): "));

        userService.register(user, readPassword("Password (min 8 chars): "), details);
        System.out.println("Registration successful. You can now log in.");
    }

    private void login() throws SQLException {
        User user = userService.login(prompt("Username: "), readPassword("Password: "));
        System.out.println("Welcome, " + user.getUsername() + "!");
    }

    private void safely(Action action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Something went wrong, please try again.");
            e.printStackTrace();
        }
    }

    private String prompt(String label) {
        System.out.print(label);
        return scanner.nextLine().trim();
    }

    private String readPassword(String label) {
        Console console = System.console();
        return console == null ? prompt(label) : new String(console.readPassword(label));
    }

    private LocalDate readDate(String label) {
        try {
            LocalDate date = LocalDate.parse(prompt(label));
            if (date.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Birthdate cannot be in the future");
            }
            return date;
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Use the format yyyy-MM-dd, e.g. 2000-05-31");
        }
    }
}
