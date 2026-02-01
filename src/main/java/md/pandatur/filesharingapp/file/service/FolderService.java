package md.pandatur.filesharingapp.file.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import md.pandatur.filesharingapp.auth.model.User;
import md.pandatur.filesharingapp.auth.repository.UserRepository;
import md.pandatur.filesharingapp.file.dto.AddFileToFolderRequest;
import md.pandatur.filesharingapp.file.dto.CreateFolderRequest;
import md.pandatur.filesharingapp.file.dto.FolderResponse;
import md.pandatur.filesharingapp.file.dto.UpdateFolderRequest;
import md.pandatur.filesharingapp.file.model.File;
import md.pandatur.filesharingapp.file.model.Folder;
import md.pandatur.filesharingapp.file.repository.FileRepository;
import md.pandatur.filesharingapp.file.repository.FolderRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    @Transactional
    public FolderResponse createFolder(final CreateFolderRequest request, final String createdByUserId) {
        // Get users
        final var createdBy = this.userRepository.findById(createdByUserId)
                .orElseThrow(() -> new RuntimeException("Created by user not found"));
        final var owner = this.userRepository.findById(request.ownerId())
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        // Create folder entity
        final var folder = this.folderRepository.save(Folder.builder()
                .name(request.name())
                .createdBy(createdBy)
                .owner(owner)
                .build());
        return mapToResponse(folder);
    }

    @Transactional
    public List<FolderResponse> getAllFoldersByUserId(final String userId) {
        final var ownedFolders = this.folderRepository.findAllByOwnerId(userId);
        final var sharedFolders = this.folderRepository.findAllBySharedToId(userId);
        final var folders = new ArrayList<Folder>();
        if (ownedFolders != null) {
            folders.addAll(ownedFolders);
        }
        if (sharedFolders != null) {
            folders.addAll(sharedFolders);
        }

        return folders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public FolderResponse getFolderById(final String folderId) {
        final var folder = this.folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        return mapToResponse(folder);
    }

    @Transactional
    public FolderResponse updateFolder(final String folderId, final UpdateFolderRequest request, final String requesterUserId) {
        final var folder = this.folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        if (!folder.getOwner().getId().equals(requesterUserId)) {
            throw new RuntimeException("User is not the owner of this folder");
        }

        if (request.name() != null) {
            folder.setName(request.name());
        }

        this.folderRepository.save(folder);
        return mapToResponse(folder);
    }

    @Transactional
    public void deleteFolder(final String folderId, final String requesterUserId) {
        final var folder = this.folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        if (!folder.getOwner().getId().equals(requesterUserId)) {
            throw new RuntimeException("User is not the owner of this folder");
        }

        // Unlink files from folder before deletion
        this.fileRepository.unlinkFilesFromFolder(folderId);

        this.folderRepository.delete(folder);
    }

    @Transactional
    public void deleteAllFoldersByUserId(final String userId) {
        this.folderRepository.deleteAllByOwnerId(userId);
    }

    @Transactional
    public FolderResponse addFileToFolder(final AddFileToFolderRequest request, final String requesterUserId) {
        final var file = this.fileRepository.findById(request.fileId())
                .orElseThrow(() -> new RuntimeException("File not found"));
        final var folder = this.folderRepository.findById(request.folderId())
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        if (!folder.getOwner().getId().equals(requesterUserId)) {
            throw new RuntimeException("User is not the owner of this folder");
        }

        file.setFolder(folder);
        this.fileRepository.save(file);

        return mapToResponse(folder);
    }

    @Transactional
    public FolderResponse removeFileFromFolder(final String fileId, final String requesterUserId) {
        final var file = this.fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        final var folder = file.getFolder();
        if (folder == null) {
            throw new RuntimeException("File is not in any folder");
        }

        if (!folder.getOwner().getId().equals(requesterUserId)) {
            throw new RuntimeException("User is not the owner of this folder");
        }

        file.setFolder(null);
        this.fileRepository.save(file);

        return mapToResponse(folder);
    }

    @Transactional
    public FolderResponse shareFolder(final String folderId, final String userId, final String requesterUserId) {
        final var folder = this.folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        final var user = this.userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!folder.getOwner().getId().equals(requesterUserId)) {
            throw new RuntimeException("User is not the owner of this folder");
        }

        folder.getSharedTo().add(user);
        this.folderRepository.save(folder);

        return mapToResponse(folder);
    }

    @Transactional
    public FolderResponse unshareFolder(final String folderId, final String userId, final String requesterUserId) {
        final var folder = this.folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        final var user = this.userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!folder.getOwner().getId().equals(requesterUserId)) {
            throw new RuntimeException("User is not the owner of this folder");
        }

        folder.getSharedTo().remove(user);
        this.folderRepository.save(folder);

        return mapToResponse(folder);
    }

    @Transactional
    public FolderResponse transferFolderOwnership(final String folderId, final String newOwnerId, final String requesterUserId) {
        final var folder = this.folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        final var newOwner = this.userRepository.findById(newOwnerId)
                .orElseThrow(() -> new RuntimeException("New owner not found"));

        if (!folder.getOwner().getId().equals(requesterUserId)) {
            throw new RuntimeException("User is not the owner of this folder");
        }

        // Transfer ownership of all files in folder to new owner
        this.fileRepository.transferFilesOwnershipByFolderId(folderId, newOwnerId);

        folder.setOwner(newOwner);
        this.folderRepository.save(folder);

        return mapToResponse(folder);
    }

    private FolderResponse mapToResponse(final Folder folder) {
        final var fileIds = folder.getFiles().stream()
                .map(File::getId)
                .collect(Collectors.toSet());

        final var sharedToUserIds = folder.getSharedTo().stream()
                .map(User::getId)
                .collect(Collectors.toSet());

        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .createdById(folder.getCreatedBy().getId())
                .createdByUsername(folder.getCreatedBy().getUsername())
                .ownerId(folder.getOwner().getId())
                .ownerUsername(folder.getOwner().getUsername())
                .fileIds(fileIds)
                .sharedToUserIds(sharedToUserIds)
                .createdAt(folder.getCreatedAt())
                .build();
    }
}
