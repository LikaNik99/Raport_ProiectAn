package crm.demo.Repositories;

import crm.demo.Enteties.Beneficiary;
import crm.demo.Enteties.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BenefeciaryRepository extends JpaRepository<Beneficiary, Long> {
    Optional<Beneficiary> findByBenificiatyFirstName(String name);
    Optional<Beneficiary> findByCompanyName(String company);
}
