package crm.demo.Repositories;

import crm.demo.Enteties.CrmUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import crm.demo.Enteties.Logs;

import java.util.List;

@Repository
public interface LogsRepository extends JpaRepository<Logs, Long>{

    List<Logs> findByUserId(Long id);
    List<Logs> findByProjectId(Long id);
    void deleteById(Long id);
}
