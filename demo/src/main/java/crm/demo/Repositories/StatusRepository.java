package crm.demo.Repositories;



import crm.demo.Enteties.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface StatusRepository extends JpaRepository<Status, Long>{
    Optional<Status> findByCode(String Code);
    
}