package crm.demo.Services;

//import crm.demo.Configutaion.SecurityConfiguration.UserAuthProvider;
import crm.demo.DTOs.SecurityDTOs.JwtAuthenticationResponse;
import crm.demo.DTOs.SecurityDTOs.SignInDto;
import crm.demo.DTOs.SecurityDTOs.SignUpDto;
import crm.demo.Enteties.CrmUser;
import crm.demo.Enteties.Customer;
import crm.demo.Enums.RoleEnum;
import crm.demo.Repositories.CrmUserRepository;
import crm.demo.Repositories.CustomerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final CustomerRepository customerRepository;
    private final CrmUserRepository userRepository;
    /**
     * Регистрация пользователя
     *
     * @param request данные пользователя
     * @return токен
     */
    @Transactional
    public JwtAuthenticationResponse signUp(SignUpDto request) {

        //  создаём user
        CrmUser user = new CrmUser();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setLogin(request.getLogin());
        user.setEmailAddress(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(RoleEnum.USER);

        //  создаём customer И СРАЗУ привязываем user
        Customer customer = new Customer();
        customer.setNickName(request.getLogin());
        customer.setCrmUser(user);

        // ОБРАТНАЯ СВЯЗЬ (КРИТИЧНО)
        user.setCustomer(customer);

        // сохраняем ВЛАДЕЛЬЦА
        customerRepository.save(customer);

        // JWT
        String token = jwtService.generateAccessToken(user);
        return new JwtAuthenticationResponse(token);
    }

    /**
     * Аутентификация пользователя
     *
     * @param request данные пользователя
     * @return токен
     */

    public  JwtAuthenticationResponse signIn(SignInDto request){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getLogin(),
                        request.getPassword()
                )
        );

        CrmUser user = (CrmUser) userService
                .userDetailsService()
                .loadUserByUsername(request.getLogin());

        String token = jwtService.generateAccessToken(user);
        return new JwtAuthenticationResponse(token);
    }

}