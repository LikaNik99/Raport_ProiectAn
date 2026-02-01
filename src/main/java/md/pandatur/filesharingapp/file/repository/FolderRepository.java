package md.pandatur.filesharingapp.file.repository;

import md.pandatur.filesharingapp.file.model.Folder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<Folder, String> {
    @EntityGraph(attributePaths = {"files", "sharedTo", "createdBy", "owner"})
    List<Folder> findAllByOwnerId(String ownerId);

    @EntityGraph(attributePaths = {"files", "sharedTo", "createdBy", "owner"})
    List<Folder> findAllBySharedToId(String userId);

    void deleteAllByOwnerId(String ownerId);
}
