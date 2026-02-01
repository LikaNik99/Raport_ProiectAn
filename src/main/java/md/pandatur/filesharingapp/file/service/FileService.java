package md.pandatur.filesharingapp.file.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import md.pandatur.filesharingapp.auth.repository.UserRepository;
import md.pandatur.filesharingapp.file.dto.CreateFileRequest;
import md.pandatur.filesharingapp.file.dto.FileResponse;
import md.pandatur.filesharingapp.file.dto.UpdateFileRequest;
import md.pandatur.filesharingapp.file.model.File;
import md.pandatur.filesharingapp.file.model.Folder;
import md.pandatur.filesharingapp.file.repository.FileRepository;
import md.pandatur.filesharingapp.file.repository.FolderRepository;
import md.pandatur.filesharingapp.storage.cloud.CloudStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final CloudStorageService cloudStorageService;

    @Transactional
    public FileResponse createFile(
            final MultipartFile multipartFile,
            final CreateFileRequest request,
            final String userId) {
        // Upload file to cloud storage
        final var fileUrl = this.cloudStorageService.uploadFile(multipartFile);

        // Get users
        final var uploadedBy = this.userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Uploaded by user not found"));
        final var owner = this.userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        // Get folder if specified
        Folder folder = null;
        if (request.folderId() != null) {
            folder = this.folderRepository.findById(request.folderId())
                    .orElseThrow(() -> new RuntimeException("Folder not found"));
        }

        // Create file entity
        final var file = this.fileRepository.save(File.builder()
                .name(request.name())
                .size((int) multipartFile.getSize())
                .url(fileUrl)
                .uploadedBy(uploadedBy)
                .owner(owner)
                .folder(folder)
                .build());
        return mapToResponse(file);
    }

    public List<FileResponse> getAllFilesByUserId(final String userId) {
        return this.fileRepository.findAllByOwnerId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<FileResponse> getFilesByFolderId(final String folderId) {
        return this.fileRepository.findAllByFolderId(folderId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public FileResponse getFileById(final String fileId) {
        final var file = this.fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        return mapToResponse(file);
    }

    @Transactional
    public FileResponse updateFile(final String fileId, final UpdateFileRequest request, final String requesterUserId) {
        final var file = this.fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getOwner().getId().equals(requesterUserId)) {
            throw new RuntimeException("User is not the owner of this file");
        }

        if (request.name() != null) {
            file.setName(request.name());
        }

        if (request.folderId() != null) {
            final var folder = this.folderRepository.findById(request.folderId())
                    .orElseThrow(() -> new RuntimeException("Folder not found"));
            file.setFolder(folder);
        }

        this.fileRepository.save(file);
        return mapToResponse(file);
    }

    @Transactional
    public void deleteFile(final String fileId, final String requesterUserId) {
        final var file = this.fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getOwner().getId().equals(requesterUserId)) {
            throw new RuntimeException("User is not the owner of this file");
        }

        // Delete from cloud storage
        this.cloudStorageService.deleteFile(file.getUrl());

        // Delete from database
        this.fileRepository.delete(file);
    }

    @Transactional
    public void deleteAllFilesByUserId(final String userId) {
        final var files = this.fileRepository.findAllByOwnerId(userId);

        // Delete all files from cloud storage
        files.forEach(file -> this.cloudStorageService.deleteFile(file.getUrl()));

        // Delete all files from database
        this.fileRepository.deleteAllByOwnerId(userId);
    }

    @Transactional
    public FileResponse transferFileOwnership(final String fileId, final String newOwnerId, final String requesterUserId) {
        final var file = this.fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        final var newOwner = this.userRepository.findById(newOwnerId)
                .orElseThrow(() -> new RuntimeException("New owner not found"));

        if (!file.getOwner().getId().equals(requesterUserId)) {
            throw new RuntimeException("User is not the owner of this file");
        }

        file.setOwner(newOwner);
        this.fileRepository.save(file);

        return mapToResponse(file);
    }

    private FileResponse mapToResponse(final File file) {
        return FileResponse.builder()
                .id(file.getId())
                .name(file.getName())
                .size(file.getSize())
                .url(file.getUrl())
                .uploadedById(file.getUploadedBy().getId())
                .uploadedByUsername(file.getUploadedBy().getUsername())
                .ownerId(file.getOwner().getId())
                .ownerUsername(file.getOwner().getUsername())
                .folderId(file.getFolder() != null ? file.getFolder().getId() : null)
                .createdAt(file.getCreatedAt())
                .build();
    }
}
