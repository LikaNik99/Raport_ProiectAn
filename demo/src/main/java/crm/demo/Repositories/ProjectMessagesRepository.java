package crm.demo.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import crm.demo.Enteties.ProjectMessages;

import java.util.List;

@Repository
public interface ProjectMessagesRepository extends JpaRepository<ProjectMessages, Long>{
    List<ProjectMessages> findAllByProjectId(Long id);

    List<ProjectMessages> findByProjectId(Long id);
}
