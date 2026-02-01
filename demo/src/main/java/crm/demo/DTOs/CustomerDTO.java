package crm.demo.DTOs;

import crm.demo.Enteties.Customer;


public record CustomerDTO(
        Long id,
        String nickName
) {
public static CustomerDTO from(Customer customer){
    return new CustomerDTO(
            customer.getId(),
            customer.getNickName()
    );
}
}