package md.pandatur.filesharingapp.user.controller;

import lombok.RequiredArgsConstructor;
import md.pandatur.filesharingapp.auth.security.UserDetailsImpl;
import md.pandatur.filesharingapp.user.dto.UserDTO;
import md.pandatur.filesharingapp.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/search")
    public ResponseEntity<UserDTO> searchUser(@RequestParam final String username) {
        return ResponseEntity.ok(this.userService.searchUser(username));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getUser(@AuthenticationPrincipal final UserDetailsImpl userDetails) {
        return ResponseEntity.ok(this.userService.getUser(userDetails.getId()));
    }

    @GetMapping("/check-availability")
    public ResponseEntity<Boolean> checkUsernameAvailability(@RequestParam final String username) {
        return ResponseEntity.ok(this.userService.isUsernameAvailable(username));
    }
}
