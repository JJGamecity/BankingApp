package com.example.bankingapp.Account;


import com.example.bankingapp.User.User;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(unique = true, nullable = false)
    private String accountNumber;
    private String routingNumber;

    private Long availableBalance = 0L;

    protected Account(){

    }

    public Account(User user, String accountNumber, String routingNumber) {
        this.user = user;
        this.accountNumber=accountNumber;
        this.routingNumber=routingNumber;
    }

    public Long CheckBalance() {
        return availableBalance;
    }
  /* public Long DepositCheck(Check check) {
        return availableBalance;
    }
*/

}
