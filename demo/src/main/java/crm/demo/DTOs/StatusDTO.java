package crm.demo.DTOs;

import crm.demo.Enteties.Status;
public record StatusDTO(
        String code,
        String displayName) {
    public static StatusDTO from(Status status){
        return new StatusDTO(
                status.getCode(),
                status.getDisplayName()
        );
    }
}
