package crm.demo.Enteties;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name="refresh_tokens")
@Getter
@Setter
public class RefreshToken {
    @Id
    @GeneratedValue
    private Long id;
    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne
    private CrmUser user;

    private Instant expiresAt;

    private boolean revoked = false;
}
