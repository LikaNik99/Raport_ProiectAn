package md.pandatur.filesharingapp.file.dto;

import lombok.Builder;

@Builder
public record CreateFileRequest(
        String name,
        String folderId
) {
}
