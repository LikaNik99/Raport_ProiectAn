package crm.demo.DTOs;

import lombok.*;

import java.time.LocalDate;
@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectUpdateRequest {
    private String projectName;
    private String projectDescription;
    private LocalDate deadLine;
    private Long ownerId;
    private String statusCode;

    // getters and setters
}