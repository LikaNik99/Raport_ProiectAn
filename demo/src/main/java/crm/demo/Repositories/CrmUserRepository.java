package crm.demo.Repositories;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import crm.demo.Enteties.CrmUser;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrmUserRepository extends JpaRepository<CrmUser, Long>{
    boolean existsByLogin(String login);
//    boolean existsByEmailAddress(String emailAddress);
    List<CrmUser> findAll();

    CrmUser findByFirstName(String name);

    Optional<CrmUser> findByLogin(String username);

    boolean existsByEmailAddress(@Size(max = 255, message = "User Login should contain not more than 255 characters") @NotBlank(message = "User Login should not be blank") @Email(message = "Email should be like 'user@example.com'") String email);
}
