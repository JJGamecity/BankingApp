package com.example.bankingapp.Account;

import com.example.bankingapp.Account.Dtos.AccountRequest;
import com.example.bankingapp.Account.Dtos.AccountResponse;
import com.example.bankingapp.User.User;
import com.example.bankingapp.User.UserNotFoundException;
import com.example.bankingapp.User.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts(Long id) {
        List<Account> accountList = accountRepository.findByUserIdOrderByIdAsc(id);
        return accountList.stream().map(accountMapper::toResponseDto).toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('CUSTOMER') and #userId == principal.userId")
    public AccountResponse getCustomerAccount(Long accountId, Long userId) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return accountMapper.toResponseDto(account);
    }

    public void changeAccount( Long id, AccountRequest request) {

        Account account = accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
        accountMapper.updateEntityFromDto(request, account);
        accountRepository.save(account);


    }
}
