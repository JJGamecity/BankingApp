package com.example.bankingapp.Account;

import com.example.bankingapp.Account.Dtos.AccountRequest;
import com.example.bankingapp.Account.Dtos.AccountResponse;
import jakarta.validation.Valid;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountRequest request) {

        AccountResponse response = accountService.createAccount(request);

        URI location = URI.create("/accounts/"+ response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/user-{id}")
    public ResponseEntity<List<AccountResponse>> getAccounts(@PathVariable Long id)
    {
        List<AccountResponse> accountsList = accountService.getAccounts(id);
        return ResponseEntity.ok().body(accountsList);
    }
}
