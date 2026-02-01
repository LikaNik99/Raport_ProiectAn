package md.pandatur.filesharingapp.file.repository;

import md.pandatur.filesharingapp.file.model.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, String> {
    List<File> findAllByOwnerId(String ownerId);
    void deleteAllByOwnerId(String ownerId);
    List<File> findAllByFolderId(String folderId);

    @Modifying
    @Query("UPDATE File f SET f.folder = null WHERE f.folder.id = :folderId")
    void unlinkFilesFromFolder(@Param("folderId") String folderId);

    @Modifying
    @Query("UPDATE File f SET f.owner.id = :newOwnerId WHERE f.folder.id = :folderId")
    void transferFilesOwnershipByFolderId(@Param("folderId") String folderId, @Param("newOwnerId") String newOwnerId);
}