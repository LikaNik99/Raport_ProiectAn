package crm.demo.Components;

import crm.demo.DTOs.*;
import crm.demo.Enteties.CrmUser;
import crm.demo.Enteties.Customer;
import crm.demo.Enteties.Project;
import crm.demo.Repositories.CrmUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000, http://172.20.130.242:3000")
@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
public class CRMUserComponent {
    private final CrmUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @GetMapping("users")
    public List<UserByIDDTO> usersList(){
        return userRepository.findAll()
                .stream()
                .map(user -> {
                    Customer customer = user.getCustomer();
                    return new UserByIDDTO(
                            user.getId(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getEmailAddress(),
                            user.getLogin(),
                            user.getAddress(),
                            user.getCity(),
                            user.getCountry(),
                            user.getPhoneNumber(),
                            user.getIsBlocked(),
                            user.getRole().name(),
                            user.getIsActive(),
                            user.getIsAccountExpired(),
                            new CustomerDTO(
                                    customer.getId(),
                                    customer.getNickName()
                            )
                    );
                })
                .toList(); // or .collect(Collectors.toList())
    }
    @GetMapping("/current-user")
    public CurrentUserDTO  getCurrentUser(Principal principal) {
        // You can use principal.getName() to get the username
        // Then load the user from your database to get the role
        CrmUser user = userRepository.findByLogin(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return CurrentUserDTO.from(user);
    }
    @GetMapping("users/{id}")
    public UserByIDDTO getUserById(@PathVariable Long id){
        CrmUser user = userRepository.findById(id).orElseThrow(()->new RuntimeException(" There is no user with such id -  "+id));
        Customer customer = user.getCustomer();
        return new  UserByIDDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmailAddress(),
                user.getLogin(),
                user.getAddress(),
                user.getCity(),
                user.getCountry(),
                user.getPhoneNumber(),
                user.getIsBlocked(),
                user.getRole().name(),
                user.getIsActive(),
                user.getIsAccountExpired(),
                new CustomerDTO(
                        customer.getId(),
                        customer.getNickName()
                )
        );
    }
    @GetMapping("my/user")
    public CurrentUserDTO getUserByPrincipal(Authentication authentication){
         CrmUser user = userRepository.findByLogin(authentication.getName()).orElseThrow(()-> new RuntimeException("No such Login Name....."));
        Customer customer = user.getCustomer();
         return new CurrentUserDTO(
                 user.getId(),
                 user.getLogin(),
                 user.getFirstName(),
                 user.getLastName(),
                 user.getRole().name(),
                 new CustomerDTO(
                         customer.getId(),
                         customer.getNickName()
                 )
         );
    }
    @Transactional
    @PutMapping("my/user")
    public CurrentUserUpdateDto updateUser(Authentication authentication, @RequestBody CurrentUserUpdateDto dto){
        CrmUser user = userRepository.findByLogin(authentication.getName())
                .orElseThrow();
        if (dto.firstName() != null) user.setFirstName(dto.firstName());
        if (dto.lastName() != null) user.setLastName(dto.lastName());
        if (dto.emailAddress() != null) user.setEmailAddress(dto.emailAddress());
        if (dto.address() != null) user.setAddress(dto.address());
        if (dto.city() != null) user.setCity(dto.city());
        if (dto.country() != null) user.setCountry(dto.country());
        if (dto.phoneNumber() != null) user.setPhoneNumber(dto.phoneNumber());
        Customer customer = user.getCustomer();
        if (customer == null) {
            customer = new Customer();
            user.setCustomer(customer);
        }
        if (dto.customer() != null && dto.customer().nickName() != null) {
            customer.setNickName(dto.customer().nickName());
        }
        return CurrentUserUpdateDto.from(user);
    }
    @PutMapping("/my/password")
    public void changePassword(
            Authentication auth,
            @RequestBody ChangePasswordRequest dto
    ) {
        CrmUser user = userRepository.findByLogin(auth.getName())
                .orElseThrow();
        if (!passwordEncoder.matches(dto.oldPassword(), user.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }
        if (!dto.newPassword().equals(dto.confirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
    }
    @PostMapping("user-add")
    public CrmUser postNewUser(@RequestBody CrmUser newUser){
        return userRepository.save(newUser);
    }

    @PutMapping("user-edit/{id}")
    public CrmUser editUserByID(@RequestBody CrmUser newUser, @PathVariable Long id){
        CrmUser userToUpdate = userRepository.findById(id).orElseThrow(()->new RuntimeException(" There is no user with such id -  "+id));
        userToUpdate.setFirstName(newUser.getFirstName());
        userToUpdate.setLastName(newUser.getLastName());
        userToUpdate.setPassword(newUser.getPassword());
        userToUpdate.setLogin(newUser.getLogin());
        userToUpdate.setRole(newUser.getRole());
        userToUpdate.setIsActive(newUser.getIsActive());
        userToUpdate.setIsBlocked(newUser.getIsBlocked());
        userToUpdate.setEmailAddress((newUser.getEmailAddress()));
        userToUpdate.setAddress(newUser.getAddress());
        userToUpdate.setCity(newUser.getCity());
        userToUpdate.setCountry(newUser.getCountry());
        userToUpdate.setPhoneNumber(newUser.getPhoneNumber());

        return userRepository.save(userToUpdate);
    }
    @DeleteMapping("user-delete/{id}")
    public void deleteUserById(@PathVariable Long id){
        userRepository.deleteById(id);
    }
}
