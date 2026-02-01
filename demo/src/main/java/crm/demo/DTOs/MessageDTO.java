package crm.demo.DTOs;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class MessageDTO {
    Long id;
    String messageContent;
    String author;
    LocalDate messageDate;


}
