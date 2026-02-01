package md.pandatur.filesharingapp.file.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record FileResponse(
        String id,
        String name,
        Integer size,
        String url,
        String uploadedById,
        String uploadedByUsername,
        String ownerId,
        String ownerUsername,
        String folderId,
        LocalDateTime createdAt
) {
}
