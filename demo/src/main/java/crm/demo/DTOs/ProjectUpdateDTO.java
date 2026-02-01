package crm.demo.DTOs;

import crm.demo.Enteties.Project;
import crm.demo.Enteties.Status;
import crm.demo.Enteties.Customer;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
@Getter
@Setter
public class ProjectUpdateDTO{
    private Long id;
    private String projectName;
    private String projectDescription;
    private LocalDate deadLine;
    private LocalDate createdOn;
    private String statusCode;
    private String statusName;
    private Long ownerId;
    private String ownerName;
    public static ProjectUpdateDTO from(Project project) {
        ProjectUpdateDTO dto = new ProjectUpdateDTO();
        dto.id = project.getId();
        dto.projectName = project.getProjectName();
        dto.projectDescription = project.getProjectDescription();
        dto.deadLine = project.getDeadLine();
        dto.createdOn = project.getCreatedOn();
        Status status = project.getStatus();
        dto.statusCode = status.getCode();
        dto.statusName = status.getDisplayName();
        if (project.getOwner() != null) {
            dto.ownerId = project.getOwner().getId();
            dto.ownerName = project.getOwner().getNickName();
        }
        return dto;
    }


}
