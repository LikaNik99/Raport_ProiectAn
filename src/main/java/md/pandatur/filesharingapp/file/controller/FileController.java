package md.pandatur.filesharingapp.file.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import md.pandatur.filesharingapp.auth.security.UserDetailsImpl;
import md.pandatur.filesharingapp.file.dto.CreateFileRequest;
import md.pandatur.filesharingapp.file.dto.FileResponse;
import md.pandatur.filesharingapp.file.dto.UpdateFileRequest;
import md.pandatur.filesharingapp.file.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @PostMapping
    public ResponseEntity<FileResponse> createFile(
            @RequestParam("file") final MultipartFile file,
            @RequestParam("name") final String name,
            @RequestParam(value = "folderId", required = false) final String folderId,
            @AuthenticationPrincipal final UserDetailsImpl userDetails
    ) {
        final var request = CreateFileRequest.builder()
                .name(name)
                .folderId(folderId)
                .build();
        return ResponseEntity.ok(this.fileService.createFile(file, request, userDetails.getId()));
    }

    @GetMapping
    public ResponseEntity<List<FileResponse>> getAllFiles(
            @AuthenticationPrincipal final UserDetailsImpl userDetails
    ) {
        return ResponseEntity.ok(this.fileService.getAllFilesByUserId(userDetails.getId()));
    }

    @GetMapping("/folder/{folderId}")
    public ResponseEntity<List<FileResponse>> getFilesByFolderId(
            @PathVariable final String folderId
    ) {
        return ResponseEntity.ok(this.fileService.getFilesByFolderId(folderId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileResponse> getFileById(
            @PathVariable final String id
    ) {
        return ResponseEntity.ok(this.fileService.getFileById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FileResponse> updateFile(
            @PathVariable final String id,
            @Valid @RequestBody final UpdateFileRequest request,
            @AuthenticationPrincipal final UserDetailsImpl userDetails
    ) {
        return ResponseEntity.ok(this.fileService.updateFile(id, request, userDetails.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable final String id,
            @AuthenticationPrincipal final UserDetailsImpl userDetails
    ) {
        this.fileService.deleteFile(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllFiles(
            @AuthenticationPrincipal final UserDetailsImpl userDetails
    ) {
        this.fileService.deleteAllFilesByUserId(userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/transfer")
    public ResponseEntity<FileResponse> transferFileOwnership(
            @PathVariable final String id,
            @RequestParam final String newOwnerId,
            @AuthenticationPrincipal final UserDetailsImpl userDetails
    ) {
        return ResponseEntity.ok(this.fileService.transferFileOwnership(id, newOwnerId, userDetails.getId()));
    }
}
