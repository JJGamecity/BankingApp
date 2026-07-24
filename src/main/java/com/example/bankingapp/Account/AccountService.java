package com.example.bankingapp.Account;

import com.example.bankingapp.Account.Dtos.AccountRequest;
import com.example.bankingapp.Account.Dtos.AccountResponse;
import com.example.bankingapp.Account.AccountMapper;
import com.example.bankingapp.User.User;
import com.example.bankingapp.User.UserNotFoundException;
import com.example.bankingapp.User.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AccountService {

    private final AccountMapper accountMapper;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final String routingNumber;
    public AccountService( @Value("${bank.routing-number}") String routingNumber, AccountMapper accountMapper, UserRepository userRepository, AccountRepository accountRepository) {
        this.accountMapper = accountMapper;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.routingNumber = routingNumber;
    }


    public AccountResponse createAccount(AccountRequest request) {
        Account account = accountMapper.toEntity(request); // maps accountNumber, routingNumber, etc. — ignores userId
        User user = userRepository.findById(request.userId())
                .orElseThrow(
                        () -> new UserNotFoundException(request.userId()));
        account.setUser(user);
        account.setRoutingNumber(routingNumber);
        accountRepository.save(account);
        return accountMapper.toResponseDto(account);
    }
    public List<AccountResponse> getAccounts(Long id) {
        List<Account> accountList = accountRepository.findByUserId(id);
        List<AccountResponse> accountResponses = new ArrayList<AccountResponse>();
        for(Account account : accountList) {
            accountResponses.add(accountMapper.toResponseDto(account));
        }
        return accountResponses;
    }
}
