package crm.demo.DTOs;

import crm.demo.Enteties.CrmUser;

public record CurrentUserUpdateDto(
        String firstName,
        String lastName,
        String emailAddress,
        String address,
        String city,
        String country,
        String phoneNumber,
        CustomerUpdateDTO customer
) {
    public static CurrentUserUpdateDto from(CrmUser user){
        return new CurrentUserUpdateDto(
                user.getFirstName(),
                user.getLastName(),
                user.getEmailAddress(),
                user.getAddress(),
                user.getCity(),
                user.getCountry(),
                user.getPhoneNumber(),
                user.getCustomer() != null
                        ? new CustomerUpdateDTO(
                        user.getCustomer().getId(),
                        user.getCustomer().getNickName()
                )
                        : null
        );
    }
}