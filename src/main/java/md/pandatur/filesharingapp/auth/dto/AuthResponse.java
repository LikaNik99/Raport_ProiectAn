package md.pandatur.filesharingapp.auth.dto;

import lombok.Builder;

@Builder
public record AuthResponse(
        String id,
        String token,
        String username,
        String name,
        String surname
) {}
