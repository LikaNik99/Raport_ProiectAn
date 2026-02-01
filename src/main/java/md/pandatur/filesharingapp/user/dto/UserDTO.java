package md.pandatur.filesharingapp.user.dto;

import lombok.Builder;

@Builder
public record UserDTO(
        String id,
        String username,
        String name,
        String surname
) {}
