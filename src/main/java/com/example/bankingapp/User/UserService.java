package com.example.bankingapp.User;



import com.example.bankingapp.User.DTOs.UserRequest;
import com.example.bankingapp.User.DTOs.UserResponse;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;


@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id).orElseThrow(()-> new UserNotFoundException(id));
        return userMapper.toResponseDto(user);
    }

    public UserResponse createUser(UserRequest request) {

        User newUser = userMapper.toEntity(request);
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
        userMapper.updateEntityFromDto(request, currentUser);
        userRepository.save(currentUser);


    }
}
