package crm.demo.DTOs;

import crm.demo.Enteties.Project;

import java.time.LocalDate;

public record ProjectDTO(
        Long id,
        String projectName,
        LocalDate deadLine,
        String projectDescription,
        LocalDate createdOn,
        String owner,
        String creatorName,
        String status,
        String benefeciary,
        String city,
        Long budget

) {
    public static ProjectDTO from(Project project){
        return new ProjectDTO(
                project.getId(),
                project.getProjectName(),
                project.getDeadLine(),
                project.getProjectDescription(),
                project.getCreatedOn(),
                project.getOwner().getNickName(),
                project.getCreatorName(),
                project.getStatus().getDisplayName(),
                project.getBeneficiary().getCompanyName(),
                project.getCity().getCity(),
                project.getBudget()
        );
    }
}
