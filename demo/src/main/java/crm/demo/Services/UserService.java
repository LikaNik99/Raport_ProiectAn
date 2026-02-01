package crm.demo.Services;

import crm.demo.Enteties.CrmUser;
import crm.demo.Enums.RoleEnum;
import crm.demo.Exceptions.AppException;
import crm.demo.Repositories.CrmUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserService {

    private final CrmUserRepository userRepository;
    /**
     * Сохранение пользователя
     *
     * @return сохраненный пользователь
     */
    public CrmUser save(CrmUser user) {
        return userRepository.save(user);
    }

    /**
     * Создание пользователя
     *
     * @return созданный пользователь
     */
    public CrmUser createUser(CrmUser user){
        if(userRepository.existsByLogin(user.getLogin())){
            throw new RuntimeException("Login already exists");
        }
        if(userRepository.existsByEmailAddress(user.getEmailAddress())){
            throw new RuntimeException("Email already exists");
        }

        return save(user);
    }

    /**
     * Получение пользователя по имени пользователя
     *
     * @return пользователь
     */

    public CrmUser getUserByLogin(String login){
        return userRepository.findByLogin(login)
                .orElseThrow(()->new AppException("User was not found", HttpStatus.NOT_FOUND));
    }

    /**
     * Получение пользователя по имени пользователя
     * <p>
     * Нужен для Spring Security
     *
     * @return пользователь
     */
    public UserDetailsService userDetailsService(){
        return this::getUserByLogin;
    }

    /**
     * Получение текущего пользователя
     *
     * @return текущий пользователь
     */

    public CrmUser getCurrentUser(){
        var username = SecurityContextHolder.getContext().getAuthentication().getName();
        return getUserByLogin(username);
    }

    /**
     * Выдача прав администратора текущему пользователю
     * <p>
     * Нужен для демонстрации
     */

    @Deprecated
    public void getAdmin(){
        var user = getCurrentUser();
        user.setRole(RoleEnum.ADMIN);
        save(user);
    }

    public boolean existsByLogin(String login) {
        return userRepository.existsByLogin(login);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailAddress(email);
    }

}