package com.example.bankingapp.User;



import com.example.bankingapp.User.DTOs.UserRequest;
import com.example.bankingapp.User.DTOs.UserResponse;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.bankingapp.Staff.LoginNamePolicy;

import java.util.List;


@Service
@PreAuthorize("!hasRole('ADMIN')")
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final LoginNamePolicy loginNames;
    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder, LoginNamePolicy loginNames) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.loginNames = loginNames;
    }
    public List<UserResponse> getAllUsers() {
        List<User> allUsers = userRepository.findAll();
         return allUsers.stream().map(userMapper::toResponseDto).toList();
    }
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(()-> new UserNotFoundException(id));
        return userMapper.toResponseDto(user);
    }

    public UserResponse createUser(UserRequest request) {
        loginNames.requireAvailable(request.username(), null);
        User newUser = userMapper.toEntity(request);
        newUser.ChangePassword(passwordEncoder.encode(request.password()));
        userRepository.save(newUser);
        return userMapper.toResponseDto(newUser);

    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);

    }

    @Transactional
    public void replaceUser(Long id, UserRequest request) {
        User currentUser = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        loginNames.requireAvailable(request.username(), id);
        userMapper.updateEntityFromDto(request, currentUser);
        currentUser.ChangePassword(passwordEncoder.encode(request.password()));
        userRepository.save(currentUser);


    }
}
