package service;

import config.DBConnection;
import dao.AccountDao;
import dao.UserDao;
import dao.UserDetailsDao;
import dao.impl.UserDAOImp;
import dao.impl.UserDetailsDAOImp;
import exception.AccountNotFoundException;
import model.Account;
import model.User;
import model.UserDetails;
import util.PasswordUtil;
import util.Session;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AccountService {

    private static final Logger LOGGER = Logger.getLogger(AccountService.class.getName());

    private final AccountDao accountDAO;
    private final UserDao userDao = new UserDAOImp();
    private final UserDetailsDao userDetailsDao = new UserDetailsDAOImp();
    private final Scanner scanner;

    public AccountService(AccountDao accountDAO, Scanner scanner) {
        this.accountDAO = accountDAO;
        this.scanner = scanner;
    }

    // -----------------------------------------------------------------------
    // Public service methods
    // -----------------------------------------------------------------------

    public void createAccount() {
        System.out.println("\n------ CREATE NEW ACCOUNT ------");
        boolean newCustomer = !Session.isLoggedIn();

        try {
            User user = null;
            UserDetails details = null;
            String holderName;

            if (newCustomer) {
                System.out.println("\n-- Customer Information --");
                user = promptNewUser();
                details = promptUserDetails();
                holderName = details.getFullName();
                System.out.println("Account holder: " + holderName);
            } else {
                holderName = promptRequired("Enter account holder name: ");
            }

            BigDecimal initialBalance = promptAmount("Enter initial deposit amount: ");
            if (initialBalance == null) return;

            Account account = Account.builder()
                    .accountNumber(generateAccountNumber())
                    .accountName(holderName)
                    .balance(initialBalance)
                    .build();

            saveAll(user, details, account);

            if (newCustomer) Session.setCurrentUser(user);

            System.out.println("\n[SUCCESS] Account created successfully!");
            printAccountSummary(account);
            LOGGER.info("New account created: " + account.getAccountNumber() + " for " + holderName);

        } catch (IllegalArgumentException e) {
            System.out.println("[ERROR] " + e.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to create account", e);
            System.out.println("[ERROR] Failed to create account: " + e.getMessage());
        }
    }

    private void saveAll(User user, UserDetails details, Account account) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (user != null) {                       // new customer
                    userDao.insertUser(conn, user);       // sets user.getId()
                    details.setUserId(user.getId());
                    userDetailsDao.insertUserDetails(conn, details);
                    account.setUserId(user.getId());
                } else {
                    account.setUserId(Session.getCurrentUser().getId());
                }
                accountDAO.insertAccount(conn, account);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Prompts the user for an account number and displays the current balance.
     */
    public void balanceInquiry() {
        System.out.println("\n------ BALANCE INQUIRY ------");

        System.out.print("Enter account number: ");
        String accountNumber = scanner.nextLine().trim();

        try {
            Account account = findAccountOrThrow(accountNumber);
            System.out.println("\n[SUCCESS] Balance Inquiry");
            printAccountSummary(account);
        } catch (AccountNotFoundException e) {
            System.out.println("[ERROR] " + e.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Balance inquiry failed for: " + accountNumber, e);
            System.out.println("[ERROR] Database error: " + e.getMessage());
        }
    }

    /**
     * Retrieves and displays all accounts in a formatted table.
     */
    public void listAccounts() {
        System.out.println("\n------ ALL ACCOUNTS ------");

        try {
            List<Account> accounts = accountDAO.findAllAccounts();

            if (accounts.isEmpty()) {
                System.out.println("No accounts found.");
                return;
            }

            printAccountsTable(accounts);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to list accounts", e);
            System.out.println("[ERROR] Failed to retrieve accounts: " + e.getMessage());
        }
    }

    public Account findAccountOrThrow(String accountNumber)
            throws AccountNotFoundException, SQLException {

        Optional<Account> opt = accountDAO.findAccountByNumber(accountNumber);
        return opt.orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private String generateAccountNumber() {
        long seed = System.currentTimeMillis() % 1_000_000_000L;
        return String.format("ACC-%010d", seed);
    }

    private void printAccountSummary(Account account) {
        System.out.println("  Account Number : " + account.getAccountNumber());
        System.out.println("  Account Name   : " + account.getAccountName());
        System.out.printf("  Balance        : PHP %.2f%n", account.getBalance());
        if (account.getCreatedAt() != null) {
            System.out.println("  Created At     : " + account.getCreatedAt());
        }
    }

    private BigDecimal promptAmount(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        try {
            BigDecimal amount = new BigDecimal(input);
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                System.out.println("[ERROR] Amount cannot be negative.");
                return null;
            }

            return amount;
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Invalid amount: " + input);
            return null;
        }
    }

    private User promptNewUser() {
        User user = new User();
        user.setUsername(promptRequired("Username: "));
        user.setEmail(promptRequired("Email: "));
        String password = promptRequired("Password (min 8 chars): ");
        if (!user.getEmail().contains("@")) {
            throw new IllegalArgumentException("Invalid email address");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        user.setPassword(PasswordUtil.hash(password));
        return user;
    }

    private UserDetails promptUserDetails() {
        UserDetails details = new UserDetails();
        details.setFirstName(promptRequired("First name: "));
        String middle = promptRequired("Middle name (optional): ");
        details.setMiddleName(middle.isBlank() ? null : middle);
        details.setLastName(promptRequired("Last name: "));
        try {
            LocalDate birthdate = LocalDate.parse(promptRequired("Birthdate (yyyy-MM-dd): "));
            if (birthdate.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Birthdate cannot be in the future");
            }
            details.setBirthdate(birthdate);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Use the format yyyy-MM-dd, e.g. 2000-05-31");
        }
        return details;
    }

    private String promptRequired(String prompt) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();
        if (value.isEmpty() && !prompt.contains("optional")) {
            throw new IllegalArgumentException(prompt.replace(":", "") + " is required");
        }
        return value;
    }

    private void printAccountsTable(List<Account> accounts) {
        String line = "+----+-----------------------+-------------------------+---------------+---------------------+";
        System.out.println(line);
        System.out.printf("| %-2s | %-21s | %-23s | %-13s | %-19s |%n",
                "No", "Account Number", "Account Name", "Balance (PHP)", "Created At");
        System.out.println(line);

        int i = 1;
        for (Account a : accounts) {
            System.out.printf("| %-2d | %-21s | %-23s | %13.2f | %-19s |%n",
                    i++,
                    a.getAccountNumber(),
                    a.getAccountName(),
                    a.getBalance(),
                    a.getCreatedAt() != null ? a.getCreatedAt().toString().replace("T", " ") : "—"
            );
        }
        System.out.println(line);
        System.out.println("  Total accounts: " + accounts.size());
    }

}
