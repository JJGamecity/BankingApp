package com.example.bankingapp.Account;


import com.example.bankingapp.User.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;


import java.math.BigDecimal;

@Entity
@Getter
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Setter
    private User user;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Column(unique = true, nullable = false)
    @Setter
    private String accountNumber;

    @Column(nullable = false)
    @Setter
    private String routingNumber;

    @Column(name = "available_balance", precision = 19, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    protected Account(){

    }

    public Account(String accountNumber, AccountType accountType) {
        this.accountNumber = accountNumber;
        this.accountType = accountType;
    }
    public Account(User user, AccountType accountType, String accountNumber, String routingNumber) {
        this.user = user;
        this.accountNumber=accountNumber;
        this.routingNumber=routingNumber;
        this.accountType = accountType;

    }

    public Account(User user,AccountType accountType, String accountNumber, String routingNumber, BigDecimal balance) {
        this.user = user;
        this.accountNumber=accountNumber;
        this.routingNumber=routingNumber;
        this.availableBalance = balance;
        this.accountType = accountType;
    }

    public BigDecimal CheckBalance() {
        return availableBalance;
    }
  /* public Long DepositCheck(Check check) {
        return availableBalance;
    }
*/

}
