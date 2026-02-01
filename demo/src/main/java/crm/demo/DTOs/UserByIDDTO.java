package crm.demo.DTOs;

public record UserByIDDTO(
        Long id,
        String firstName,
        String lastName,
        String emailAddress,
        String login,
        String address,
        String city,
        String country,
        String phoneNumber,
        Boolean isBlocked,
        String role,
        Boolean isActive,
        Boolean isAccountExpired,
        CustomerDTO customer) {
}