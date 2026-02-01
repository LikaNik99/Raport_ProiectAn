package md.pandatur.filesharingapp.file.dto;

import lombok.Builder;

@Builder
public record CreateFolderRequest(
        String name,
        String ownerId
) {
}
