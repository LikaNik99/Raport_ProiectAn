package crm.demo.Components;

import crm.demo.DTOs.LogsDTO;
import crm.demo.DTOs.ProjectDTO;
import crm.demo.Enteties.Logs;
import crm.demo.Repositories.LogsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogsComponent {

    private final LogsRepository logsRepository;

    @GetMapping
    public List<LogsDTO> getAllLogs() {
        return logsRepository.findAll().stream()
                .map(LogsDTO::from)
                .toList();
    }
    @GetMapping("/by-user/{userId}")
    public List<Logs> getByUser(@PathVariable Long userId) {
        return logsRepository.findByUserId(userId);
    }

    @GetMapping("/by-project/{projectId}")
    public List<LogsDTO> getByProject(@PathVariable Long projectId) {
        return logsRepository.findByProjectId(projectId)
                .stream()
                .map(LogsDTO::from)
                .toList();
    }

    @DeleteMapping
    public void deleteByIds(@RequestBody List<Long> ids) {
        ids.forEach(logsRepository::deleteById);
    }
}
