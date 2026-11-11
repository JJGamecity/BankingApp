package com.example.bankingapp;

import com.example.bankingapp.Account.*;
import com.example.bankingapp.Account.Dtos.AccountResponse;
import com.example.bankingapp.User.UserRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerAccountServiceTest {
    @Test
    void lookupAlwaysIncludesOwnerAndDoesNotFallBackToUnscopedQuery() {
        var repository = mock(AccountRepository.class);
        var mapper = mock(AccountMapper.class);
        var service = new AccountService("91000019", mapper, mock(UserRepository.class), repository);
        var account = mock(Account.class);
        var response = new AccountResponse(10L, 1L, AccountType.CHECKING, "1000000001", "91000019", java.math.BigDecimal.ZERO);
        when(repository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(account));
        when(mapper.toResponseDto(account)).thenReturn(response);
        assertEquals(response, service.getCustomerAccount(10L, 1L));
        assertThrows(AccountNotFoundException.class, () -> service.getCustomerAccount(10L, 2L));
        assertThrows(AccountNotFoundException.class, () -> service.getCustomerAccount(99L, 1L));
        verify(repository, never()).findById(anyLong());
        verify(mapper, times(1)).toResponseDto(any());
    }
}
