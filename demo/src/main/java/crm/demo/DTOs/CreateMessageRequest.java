package crm.demo.DTOs;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateMessageRequest {
    private String messageContent;
    private LocalDate messageDate;
    private String author;
}