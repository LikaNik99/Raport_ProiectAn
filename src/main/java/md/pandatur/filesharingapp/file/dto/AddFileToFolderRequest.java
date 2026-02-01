package md.pandatur.filesharingapp.file.dto;

import lombok.Builder;

@Builder
public record AddFileToFolderRequest(
        String fileId,
        String folderId
) {
}
