package crm.demo.Enteties;

import java.util.Collection;
import java.util.List;

import crm.demo.Enums.RoleEnum;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@Data
@Builder
@Setter
@Entity
@Table(name = "CrmUser")
@AllArgsConstructor
@NoArgsConstructor
public class CrmUser implements UserDetails {
    
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column
    String firstName;
    
    @Column
    String lastName;

    @Column(nullable = false, unique = true)
    String login;
    
    @Column(name = "password", nullable = false)
    String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    RoleEnum role;

    @Builder.Default
    @Column(nullable = false)
    Boolean isActive = true;

    @Builder.Default
    @Column(nullable = false)
    Boolean isBlocked = false;

    @Builder.Default
    @Column(nullable = false)
    Boolean isAccountExpired = false;

    @Column(unique = true)
    String emailAddress;

    @Column()
    String address;

    @Column()
    String city;

    @Column()
    String country;

    @Column(unique = true)
    String phoneNumber;

    @OneToOne(mappedBy = "crmUser")
    Customer customer;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_"+role.name()));
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public boolean isAccountNonExpired() {
        return !isAccountExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isBlocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
    
}
