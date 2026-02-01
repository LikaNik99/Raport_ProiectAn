package crm.demo.DTOs;

import crm.demo.Enteties.ProjectMessages;

import java.time.LocalDate;

public record ProjectMessageDTO(
        Long id,
        String messageContent,
        String author,
        LocalDate messageDate
) {
    public static ProjectMessageDTO from(ProjectMessages projectMessages){
        return new ProjectMessageDTO(
                projectMessages.getId(),
                projectMessages.getMessageContent(),
                projectMessages.getAuthor(),
                projectMessages.getMessageDate()
        );
    }
}
