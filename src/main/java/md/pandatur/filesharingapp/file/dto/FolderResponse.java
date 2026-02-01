package md.pandatur.filesharingapp.file.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
public record FolderResponse(
        String id,
        String name,
        String createdById,
        String createdByUsername,
        String ownerId,
        String ownerUsername,
        Set<String> fileIds,
        Set<String> sharedToUserIds,
        LocalDateTime createdAt
) {
}
