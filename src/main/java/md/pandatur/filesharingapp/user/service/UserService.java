package md.pandatur.filesharingapp.user.service;

import lombok.RequiredArgsConstructor;
import md.pandatur.filesharingapp.auth.model.User;
import md.pandatur.filesharingapp.auth.repository.UserRepository;
import md.pandatur.filesharingapp.user.dto.UserDTO;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserDTO searchUser(final String username) {
        final User user = this.userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .surname(user.getSurname())
                .build();
    }

    public UserDTO getUser(final String userId) {
        final User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .surname(user.getSurname())
                .build();
    }

    public boolean isUsernameAvailable(final String username) {
        return !userRepository.existsByUsername(username);
    }
}
