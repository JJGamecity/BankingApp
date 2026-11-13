package com.example.bankingapp.Account;

import com.example.bankingapp.Account.Dtos.AccountRequest;
import com.example.bankingapp.Account.Dtos.AccountResponse;
import com.example.bankingapp.CustomerPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.Operation;
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

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "List your accounts", description = "Returns balances and bank details ordered by account ID. Returns an empty array when you have no accounts.")
    public ResponseEntity<List<AccountResponse>> getMyAccounts(
            @AuthenticationPrincipal CustomerPrincipal customer) {
        return ResponseEntity.ok(accountService.getAccounts(customer.getUserId()));
    }

    @GetMapping("/me/{accountId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "View one of your accounts", description = "Returns 404 if the account does not exist or belongs to another customer.")
    public ResponseEntity<AccountResponse> getMyAccount(
            @AuthenticationPrincipal CustomerPrincipal customer,
            @PathVariable @Positive Long accountId) {
        return ResponseEntity.ok(accountService.getCustomerAccount(accountId, customer.getUserId()));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountRequest request) {

        AccountResponse response = accountService.createAccount(request);

        URI location = URI.create("/accounts/"+ response.id());
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping
    public ResponseEntity<Void> changeAccount(@RequestParam @Positive Long id, @Valid @RequestBody AccountRequest request) {

        accountService.changeAccount(id,request);
        return ResponseEntity.noContent().build();

    }

    @GetMapping("/user-{id}")
    public ResponseEntity<List<AccountResponse>> getAccounts(@PathVariable Long id)
    {
        List<AccountResponse> accountsList = accountService.getAccounts(id);
        return ResponseEntity.ok().body(accountsList);
    }


}
