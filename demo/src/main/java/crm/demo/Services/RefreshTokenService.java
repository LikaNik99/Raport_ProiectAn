package crm.demo.Services;

import crm.demo.Enteties.CrmUser;
import crm.demo.Enteties.RefreshToken;
import crm.demo.Repositories.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository repo;
    public RefreshToken create(CrmUser user) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString());
        rt.setUser(user);
        rt.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        return repo.save(rt);
    }

    public RefreshToken validate(String token) {
        RefreshToken rt = repo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh"));

        if (rt.isRevoked() || rt.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh expired");
        }
        return rt;
    }

    public void revoke(String token) {
        repo.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            repo.save(rt);
        });
    }
}
