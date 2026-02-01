package md.pandatur.filesharingapp.file.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import md.pandatur.filesharingapp.auth.security.UserDetailsImpl;
import md.pandatur.filesharingapp.file.dto.AddFileToFolderRequest;
import md.pandatur.filesharingapp.file.dto.CreateFolderRequest;
import md.pandatur.filesharingapp.file.dto.FolderResponse;
import md.pandatur.filesharingapp.file.dto.UpdateFolderRequest;
import md.pandatur.filesharingapp.file.service.FolderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/folders")
@RequiredArgsConstructor
public class FolderController {
    private final FolderService folderService;

    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(
            @Valid @RequestBody final CreateFolderRequest request,
            @AuthenticationPrincipal final UserDetailsImpl userDetails
    ) {
        return ResponseEntity.ok(this.folderService.createFolder(request, userDetails.getId()));
    }

    @GetMapping
    public ResponseEntity<List<FolderResponse>> getAllFolders(
            @AuthenticationPrincipal final UserDetailsImpl userDetails
    ) {
        return ResponseEntity.ok(this.folderService.getAllFoldersByUserId(userDetails.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FolderResponse> getFolderById(
            @PathVariable final String id
    ) {
        return ResponseEntity.ok(this.folderService.getFolderById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FolderResponse> updateFolder(
            @PathVariable final String id,
            @Valid @RequestBody final UpdateFolderRequest request,
            @AuthenticationPrincipal final UserDetailsImpl userDetails
    ) {
        return ResponseEntity.ok(this.folderService.updateFolder(id, request, userDetails.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFolder(
            @PathVariable final String id,
            @AuthenticationPrincipal final UserDetailsImpl userDetails
    ) {
        this.folderService.deleteFolder(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllFolders(
            @AuthenticationPrincipal final UserDetailsImpl userDetails
    ) {
        this.folderService.deleteAllFoldersByUserId(userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/add-file")
    public ResponseEntity<FolderResponse> addFileToFolder(
            @Valid @RequestBody final AddFileToFolderRequest request,
            @AuthenticationPrincipal final UserDetailsImpl userDetails
    ) {
        return ResponseEntity.ok(this.folderService.addFileToFolder(request, userDetails.getId()));
    }

    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<FolderResponse> removeFileFromFolder(
            @PathVariable final String fileId,
            @AuthenticationPrincipal final UserDetailsImpl userDetails
    ) {
        return ResponseEntity.ok(this.folderService.removeFileFromFolder(fileId, userDetails.getId()));
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<FolderResponse> shareFolder(
            @PathVariable final String id,
            @RequestParam final String userId,
            @AuthenticationPrincipal final UserDetailsImpl userDetails
    ) {
        return ResponseEntity.ok(this.folderService.shareFolder(id, userId, userDetails.getId()));
    }

    @DeleteMapping("/{id}/share/{userId}")
    public ResponseEntity<FolderResponse> unshareFolder(
            @PathVariable final String id,
            @PathVariable final String userId,
            @AuthenticationPrincipal final UserDetailsImpl userDetails
    ) {
        return ResponseEntity.ok(this.folderService.unshareFolder(id, userId, userDetails.getId()));
    }

    @PutMapping("/{id}/transfer")
    public ResponseEntity<FolderResponse> transferFolderOwnership(
            @PathVariable final String id,
            @RequestParam final String newOwnerId,
            @AuthenticationPrincipal final UserDetailsImpl userDetails
    ) {
        return ResponseEntity.ok(this.folderService.transferFolderOwnership(id, newOwnerId, userDetails.getId()));
    }
}
