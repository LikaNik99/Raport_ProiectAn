package md.pandatur.filesharingapp.file.dto;

import lombok.Builder;

@Builder
public record UpdateFileRequest(
        String name,
        String folderId
) {
}
