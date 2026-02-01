package crm.demo.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Getter
@Setter
public class ProjectCreationRequest {
    @NotBlank
    String projectName;
    @NotNull LocalDate deadLine;
    String projectDescription;
    @NotNull Long ownerId;
    @NotNull Long cityId;
    @NotNull Long beneficiaryId;
    @NotNull
    Long budget;
    String statusCode;
}
