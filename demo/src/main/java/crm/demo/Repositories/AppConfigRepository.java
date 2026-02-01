package crm.demo.Repositories;

import crm.demo.Enteties.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface AppConfigRepository extends JpaRepository<AppConfig, Long> {
}