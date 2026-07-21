package com.example.bankingapp.User;


import com.example.bankingapp.User.DTOs.UserRequest;
import com.example.bankingapp.User.DTOs.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;


@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(UserRequest request);
    UserResponse toResponseDto(User user);
    void updateEntityFromDto(UserRequest dto, @MappingTarget User entity);
}