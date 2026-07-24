package com.example.bankingapp.Account;

import com.example.bankingapp.Account.Dtos.AccountRequest;
import com.example.bankingapp.Account.Dtos.AccountResponse;
import com.example.bankingapp.User.DTOs.UserRequest;
import com.example.bankingapp.User.DTOs.UserResponse;
import com.example.bankingapp.User.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)

public interface AccountMapper {

    @Mapping(target = "user", ignore = true)
    Account toEntity(AccountRequest request);

    @Mapping(source = "user.id", target = "userId")
    AccountResponse toResponseDto(Account account);

    void updateEntityFromDto(AccountRequest request, @MappingTarget Account entity);


}
