package model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter

public class Transaction {
    private Long id;
    private String transactionReference;
    private String accountNumber;
    private TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal balance;
    private String referenceAccount;
    private String remarks;
    private LocalDateTime createdAt;

//  ==============================
//  |  CONSTRUCTOR
//  ==============================

    public Transaction() {
    }

    public Transaction(String accountNumber, TransactionType transactionType,
                       BigDecimal amount, BigDecimal balance,
                       String referenceAccount, String remarks) {
        this.accountNumber = accountNumber;
        this.transactionType = transactionType;
        this.amount = amount;
        this.balance = balance;
        this.referenceAccount = referenceAccount;
        this.remarks = remarks;
    }

    public void setTransactionId(long id) {
        this.id = id;
    }

}
