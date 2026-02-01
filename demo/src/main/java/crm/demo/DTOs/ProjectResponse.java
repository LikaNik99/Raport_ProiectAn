package crm.demo.DTOs;

import crm.demo.Enteties.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
public record ProjectResponse(
        Long id,
        String projectName,
        String projectDescription,
        LocalDate deadLine,
        LocalDate createdOn,
        String statusCode,
        String statusName,
        String ownerName,
        Long ownerId,
        Long beneficiaryId,
        Long cityId,
        Long budget
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getProjectName(),
                project.getProjectDescription(),
                project.getDeadLine(),
                project.getCreatedOn(),
                project.getStatus().getCode(),
                project.getStatus().getDisplayName(),
                project.getCreatorName(),
                project.getOwner().getId(),
                project.getBeneficiary().getId(),
                project.getCity().getId(),
                project.getBudget()
        );
    }
}
