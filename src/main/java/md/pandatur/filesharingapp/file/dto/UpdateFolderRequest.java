package md.pandatur.filesharingapp.file.dto;

import lombok.Builder;

@Builder
public record UpdateFolderRequest(
        String name
) {
}
