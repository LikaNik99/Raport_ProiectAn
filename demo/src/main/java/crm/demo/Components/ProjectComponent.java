package crm.demo.Components;

import crm.demo.DTOs.*;
import crm.demo.Enteties.*;
import crm.demo.Repositories.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
public class ProjectComponent {

    private final ProjectRepository projectRepository;
    private final CustomerRepository customerRepository;
    private final StatusRepository statusRepository;
    private final LogsRepository logsRepository;
    private final BenefeciaryRepository benefeciaryRepository;
    private final CitiesRepository citiesRepository;
    private final ProjectMessagesRepository projectMessagesRepository;
    @GetMapping("projects")
    public List<ProjectDTO> listAllProjects() {

        return projectRepository.findAll()
                .stream()
                .map(ProjectDTO::from)
                .toList();
    }

    @GetMapping("project/{id}")
    public ProjectByIdDto getProjectById(@PathVariable Long id) {
        Project project = projectRepository.findById(id).orElseThrow(() -> new RuntimeException("No Project with such ID " + id));
        return ProjectByIdDto.from(project);
    }

    @PostMapping("project-add")
    @Transactional
    public ProjectResponse createNewProject(
            @Valid @RequestBody ProjectCreationRequest req,
            Principal principal
    ) {
        Customer owner = customerRepository.findById(req.getOwnerId())
                .orElseThrow(() -> new IllegalArgumentException("Owner not found"));

        Status status = statusRepository.findByCode(
                Optional.ofNullable(req.getStatusCode()).orElse("100")
        ).orElseThrow(() -> new IllegalStateException("Status not found"));

        City city = citiesRepository.findById(req.getCityId())
                .orElseThrow(() -> new IllegalArgumentException("City not found"));

        Beneficiary beneficiary = benefeciaryRepository.findById(req.getBeneficiaryId())
                .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found"));

        Project project = new Project();
        project.setProjectName(req.getProjectName());
        project.setDeadLine(req.getDeadLine());
        project.setProjectDescription(req.getProjectDescription());
        project.setOwner(owner);
        project.setCreatorName(principal.getName());
        project.setCreatedOn(LocalDate.now());
        project.setStatus(status);
        project.setCity(city);
        project.setBeneficiary(beneficiary);
        project.setBudget(req.getBudget());

        Project saved = projectRepository.save(project);

        Logs log = new Logs();
        log.setProject(saved);
        log.setUser(owner);
        log.setLogDateTime(LocalDate.now());
        log.setLogText("New project created by " + owner.getNickName());
        logsRepository.save(log);

        return ProjectResponse.from(saved);
    }

    @GetMapping("my/projects")
    public List<ProjectDTO> getProjectsByUser(Principal principal) {

        return projectRepository.findByOwnerNickName(principal.getName()).stream()
                .map(ProjectDTO::from)
                .toList();
    }

    @PutMapping("project-update/{id}")
    public ProjectResponse updateProject(@PathVariable Long id,
                                    @Valid @RequestBody ProjectUpdateRequest request,
                                    Principal principal) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No project with ID " + id));

        StringBuilder changes = new StringBuilder();

        // Project name
        if (request.getProjectName() != null && !request.getProjectName().isBlank()
                && !request.getProjectName().equals(project.getProjectName())) {
            changes.append("Name: ").append(project.getProjectName()).append(" -> ")
                    .append(request.getProjectName()).append("; ");
            project.setProjectName(request.getProjectName());
        }

        // Project description
        if (request.getProjectDescription() != null && !request.getProjectDescription().equals(project.getProjectDescription())) {
            changes.append("Description: ").append(project.getProjectDescription()).append(" -> ")
                    .append(request.getProjectDescription()).append("; ");
            project.setProjectDescription(request.getProjectDescription());
        }

        // Deadline
        if (request.getDeadLine() != null && !request.getDeadLine().equals(project.getDeadLine())) {
            changes.append("Deadline: ").append(project.getDeadLine()).append(" -> ")
                    .append(request.getDeadLine()).append("; ");
            project.setDeadLine(request.getDeadLine());
        }

        // Owner
        if (request.getOwnerId() != null && (project.getOwner() == null || !project.getOwner().getId().equals(request.getOwnerId()))) {
            Customer newOwner = customerRepository.findById(request.getOwnerId())
                    .orElseThrow(() -> new RuntimeException("No customer with ID " + request.getOwnerId()));
            changes.append("Owner: ").append(project.getOwner() != null ? project.getOwner().getNickName() : "null")
                    .append(" -> ").append(newOwner.getNickName()).append("; ");
            project.setOwner(newOwner);
        }

        // Status
        if (request.getStatusCode() != null && !request.getStatusCode().equals(project.getStatus().getCode())) {
            Status newStatus = statusRepository.findByCode(request.getStatusCode())
                    .orElseThrow(() -> new RuntimeException("No status with code " + request.getStatusCode()));
            changes.append("Status: ").append(project.getStatus().getDisplayName())
                    .append(" -> ").append(newStatus.getDisplayName()).append("; ");
            project.setStatus(newStatus);
        }

        Project savedProject = projectRepository.save(project);

        // Save log if changes exist
        if (changes.length() > 0) {
            Logs log = new Logs();
            log.setProject(savedProject);
            log.setUser(project.getOwner()); // owner is new owner now
            log.setLogDateTime(LocalDate.now());
            log.setLogText("Project updated: " + changes.toString() + " by " + principal.getName());
            logsRepository.save(log);
        }

        // Return DTO
        return new ProjectResponse(
                savedProject.getId(),
                savedProject.getProjectName(),
                savedProject.getProjectDescription(),
                savedProject.getDeadLine(),
                savedProject.getCreatedOn(),
                savedProject.getStatus().getCode(),
                savedProject.getStatus().getDisplayName(),
                savedProject.getOwner().getNickName(),
                savedProject.getOwner().getId(),
                savedProject.getBeneficiary().getId(),
                savedProject.getCity().getId(),
                savedProject.getBudget()
        );
    }


    @DeleteMapping("project-delete/{id}")
    public void deleteById(@PathVariable Long id) {

        projectRepository.deleteById(id);
    }


}
