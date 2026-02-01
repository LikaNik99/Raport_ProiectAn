package crm.demo.DTOs;

import crm.demo.Enteties.CrmUser;

public record CurrentUserDTO(
        Long id,
        String login,
        String firstName,
        String lastName,
        String role,
        CustomerDTO customerDTO
) {
    public static CurrentUserDTO from(CrmUser user){
        return new CurrentUserDTO(
                user.getId(),
                user.getLogin(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getCustomer() != null
                        ? new CustomerDTO(
                        user.getCustomer().getId(),
                        user.getCustomer().getNickName()
                )
                        : null
        );
    }
}