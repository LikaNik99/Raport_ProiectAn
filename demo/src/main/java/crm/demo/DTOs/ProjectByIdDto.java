package crm.demo.DTOs;

import crm.demo.Enteties.Project;

import java.time.LocalDate;

public record ProjectByIdDto(
        Long id,
        String projectName,
        String projectDescription,
        LocalDate deadLine,
        LocalDate createdOn,
        String statusCode,
        String statusName,
        Long ownerId,
        String ownerName,
        Long beneficiaryId,
        String companyName,
        Long cityId,
        String city,
        Long budget
) {
    public static ProjectByIdDto from(Project project) {
        return new ProjectByIdDto(
                project.getId(),
                project.getProjectName(),
                project.getProjectDescription(),
                project.getDeadLine(),
                project.getCreatedOn(),
                project.getStatus().getCode(),
                project.getStatus().getDisplayName(),
                project.getOwner().getId(),
                project.getOwner().getNickName(),
                project.getBeneficiary().getId(),
                project.getBeneficiary().getCompanyName(),
                project.getCity().getId(),
                project.getCity().getCity(),
                project.getBudget()
        );
    }
}