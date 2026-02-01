package crm.demo.DTOs;

public record ChangePasswordRequest(
        String oldPassword,
        String newPassword,
        String confirmPassword
) {}