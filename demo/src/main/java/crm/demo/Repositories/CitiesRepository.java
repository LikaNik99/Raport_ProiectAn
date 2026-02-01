package crm.demo.Repositories;

import crm.demo.Enteties.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CitiesRepository extends JpaRepository<City, Long> {
    Optional<City> findByCity(String city);
}
