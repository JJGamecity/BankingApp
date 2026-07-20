package com.example.bankingapp.User;


import com.example.demo.Account.Account;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Getter
    @Setter
    @Column(name = "email")
    private String email;
    @Column(name = "password")
    private String password;

    @Getter
    @Setter
    @Column(name = "username")
    private String username;

    @Getter
    @Column(name = "first_name")
    private String firstName;

    @Getter
    @Column(name = "last_name")
    private String lastName;

    @Getter
    @Setter
    @Column(name = "dob")
    private LocalDate dateOfBirth;

    @Getter
    @NotNull(message = "Credit score must be calculated before saving")
    @Min(value = 300, message = "Credit score can't be below 300")
    @Max(value = 850, message = "Credit score can't be over 850")
    @Column(name = "credit_score")
    private Integer creditScore;

    @OneToMany(mappedBy = "user")
    private List<Account> accounts = new ArrayList<>();

    public void ChangePassword(String password) {
        this.password = password;
    }
    public void ChangeFirstName(String firstName) {
        this.firstName = firstName;
    }
    public void ChangeLastName(String lastName) {
        this.lastName = lastName;
    }


    public int getAge() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
    protected User() {

    }
    public User(String email, String password, String username, LocalDate dateOfBirth, int creditScore) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.dateOfBirth = dateOfBirth;
        this.creditScore=creditScore;
    }


}
